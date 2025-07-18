package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.dto.request.GroupBuyImageUploadRequest;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyImage;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyDetailImageRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.event.GroupBuyDetailImageUploadEvent;
import com.ururulab.ururu.groupBuy.service.validation.GroupBuyValidator;
import com.ururulab.ururu.image.service.ImageHashService;
import com.ururulab.ururu.image.service.ImageService;
import com.ururulab.ururu.image.validation.ImageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;
import static com.ururulab.ururu.groupBuy.dto.validation.GroupBuyValidationConstants.MAX_GROUP_BUY_DETAIL_IMAGES;
import static com.ururulab.ururu.image.domain.ImageCategory.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupBuyDetailImageService {

    private final ImageService imageService;
    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyDetailImageRepository groupBuyDetailImageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ImageHashService imageHashService;
    private final ImageValidator imageValidator;

    /**
     * 상세이미지 업로드 이벤트 발행 (검증 + 임시 파일 생성)
     */
    public void uploadDetailImages(Long groupBuyId, List<MultipartFile> detailImageFiles) {
        if (detailImageFiles == null || detailImageFiles.isEmpty()) {
            log.warn("Detail image files are null or empty for groupBuy: {}", groupBuyId);
            return;
        }

        // 개수 체크
        if (detailImageFiles.size() > MAX_GROUP_BUY_DETAIL_IMAGES) {
            throw new BusinessException(GROUPBUY_DETAIL_IMAGES_TOO_MANY);
        }

        // 이미지 업로드 시 용량 검증
        imageValidator.validateFileSizes(detailImageFiles);

        // 이미지 검증
        imageValidator.validateAllImages(detailImageFiles);

        List<GroupBuyImageUploadRequest> imageRequests = createDetailImageRequests(detailImageFiles);

        if (!imageRequests.isEmpty()) {
            eventPublisher.publishEvent(new GroupBuyDetailImageUploadEvent(groupBuyId, imageRequests));
            log.info("Scheduled {} detail images for upload for groupBuy: {}",
                    imageRequests.size(), groupBuyId);
        }
    }

    /**
     * MultipartFile 리스트 → GroupBuyImageUploadRequest 리스트 변환 (임시 파일 기반)
     */
    private List<GroupBuyImageUploadRequest> createDetailImageRequests(List<MultipartFile> detailImageFiles) {
        AtomicInteger displayOrder = new AtomicInteger(1);
        List<GroupBuyImageUploadRequest> uploadRequests = new ArrayList<>();
        List<File> createdTempFiles = new ArrayList<>();  // 생성된 임시 파일 추적

        for (MultipartFile file : detailImageFiles) {
            if (file != null && !file.isEmpty()) {
                try {
                    String imageHash = imageHashService.calculateImageHash(file);
                    File tempFile = createTempFile(file);
                    createdTempFiles.add(tempFile); // 생성된 파일 추가
                    int order = displayOrder.getAndIncrement();

                    log.info("Detail image processed - index: {}, filename: {}, hash: {}, size: {} bytes",
                            order, file.getOriginalFilename(), imageHash, tempFile.length());

                    uploadRequests.add(new GroupBuyImageUploadRequest(
                            null, // groupBuyId - 이벤트에서 관리
                            null, // groupBuyImageId - 새로 생성이므로 null
                            file.getOriginalFilename(),
                            tempFile.getAbsolutePath(), // 임시 파일 경로
                            order, // displayOrder: 1, 2, 3, ..., 10
                            imageHash
                    ));

                } catch (Exception e) {
                    log.error("Failed to process detail image file: {}", file.getOriginalFilename(), e);
                    // 예외 발생 시 생성된 임시 파일들 정리
                    createdTempFiles.forEach(this::cleanupTempFile);
                    throw new BusinessException(IMAGE_READ_FAILED);
                }
            }
        }

        return uploadRequests;
    }

    /**
     * 비동기 상세이미지 업로드 및 DB 업데이트 (스트리밍 방식)
     */
    @Async("imageUploadExecutor")
    @Transactional
    public void uploadDetailImagesAsync(Long groupBuyId, List<GroupBuyImageUploadRequest> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        log.info("Processing {} detail images for groupBuy: {}", images.size(), groupBuyId);

        GroupBuy groupBuy = groupBuyRepository.findById(groupBuyId)
                .orElseThrow(() -> new BusinessException(GROUPBUY_NOT_FOUND, groupBuyId));

        List<GroupBuyImage> detailImages = new ArrayList<>();
        List<String> failedUploads = new ArrayList<>();

        for (GroupBuyImageUploadRequest imageRequest : images) {
            File tempFile = new File(imageRequest.tempFilePath());

            try {
                // File에서 직접 스트리밍 업로드
                String imageUrl = uploadToS3WithRetry(
                        tempFile,
                        imageRequest.originalFilename(),
                        groupBuyId,
                        imageRequest.displayOrder()
                );

                log.info("Detail image uploaded for groupBuy ID: {} -> {} (order: {})",
                        groupBuyId, imageUrl, imageRequest.displayOrder());

                // 엔티티 생성
                GroupBuyImage detailImage = GroupBuyImage.of(
                        groupBuy,
                        imageUrl,
                        imageRequest.displayOrder(),
                        false
                );

                detailImage.updateImageHash(imageUrl, imageRequest.detailImageHash());
                detailImages.add(detailImage);

            } catch (Exception e) {
                log.error("Failed to upload detail image for groupBuy ID: {} (order: {})",
                        groupBuyId, imageRequest.displayOrder(), e);
                failedUploads.add(String.format("이미지 %d번 업로드 실패: %s",
                        imageRequest.displayOrder(), e.getMessage()));
            } finally {
                // 임시 파일 정리
                cleanupTempFile(tempFile);
            }
        }

        // DB에 저장
        groupBuyDetailImageRepository.saveAll(detailImages);
        log.info("Saved {} detail images to DB for groupBuy: {}", detailImages.size(), groupBuyId);
        if (!failedUploads.isEmpty()) {
            log.warn("Failed to upload {} images for groupBuy {}: {}", failedUploads.size(), groupBuyId, failedUploads);
        }
    }

    /**
     * 임시 파일 생성
     */
    private File createTempFile(MultipartFile multipartFile) throws IOException {
        File tempFile = Files.createTempFile("detail_", ".tmp").toFile();

        multipartFile.transferTo(tempFile);

        log.debug("Created temp file: {} (size: {} bytes)", tempFile.getName(), tempFile.length());
        return tempFile;
    }

    /**
     * 임시 파일 정리
     */
    private void cleanupTempFile(File tempFile) {
        try {
            if (tempFile.exists() && tempFile.delete()) {
                log.debug("Cleaned up temp file: {}", tempFile.getName());
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup temp file: {}", tempFile.getName(), e);
        }
    }

    /**
     * 재시도 메커니즘이 적용된 S3 업로드
     */
    @Retryable(
            value = {S3Exception.class, SocketTimeoutException.class, ConnectException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    private String uploadToS3WithRetry(File tempFile, String originalFilename, Long groupBuyId, int displayOrder) {
        log.info("Attempting S3 upload for groupBuy: {} (file: {}, order: {})", groupBuyId, originalFilename, displayOrder);

        try {
            String imageUrl = imageService.uploadFileStreaming(
                    tempFile,
                    originalFilename,
                    GROUPBUY_DETAIL.getPath()
            );

            log.info("S3 upload successful for groupBuy: {} (order: {})", groupBuyId, displayOrder);
            return imageUrl;

        } catch (S3Exception e) {
            log.warn("S3 upload failed for groupBuy: {} (order: {}) - S3 Error: {}",
                    groupBuyId, displayOrder, e.getMessage());
            throw e; // 재시도를 위해 예외 재발생
        } catch (Exception e) {
            log.warn("S3 upload failed for groupBuy: {} (order: {}) - Network Error: {}",
                    groupBuyId, displayOrder, e.getMessage());
            throw new RuntimeException("S3 업로드 네트워크 오류", e);
        }
    }

    /**
     * 재시도 최종 실패 시 실행되는 복구 메서드
     */
    @Recover
    private String recoverFromS3UploadFailure(Exception ex, File tempFile, String originalFilename,
                                              Long groupBuyId, int displayOrder) {
        log.error("S3 upload final failure after all retries for groupBuy: {} (order: {}) - {}",
                groupBuyId, displayOrder, ex.getMessage());

        throw new BusinessException(IMAGE_UPLOAD_FAILED,
                String.format("상세 이미지 업로드 최종 실패 (groupBuy: %d, order: %d): %s",
                        groupBuyId, displayOrder, ex.getMessage()));
    }
}
