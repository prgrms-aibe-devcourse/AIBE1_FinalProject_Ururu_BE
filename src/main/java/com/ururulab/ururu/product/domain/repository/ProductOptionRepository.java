package com.ururulab.ururu.product.domain.repository;

import com.ururulab.ururu.product.domain.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {
    // 특정 상품의 모든 옵션 조회 (삭제되지 않은 것만)
    @Query("""
    SELECT po FROM ProductOption po 
    WHERE po.product.id = :productId AND po.isDeleted = false
    """)
    List<ProductOption> findByProductIdAndIsDeletedFalse(Long productId);

    Optional<ProductOption> findByIdAndIsDeletedFalse(Long id);

    @Modifying
    @Query("UPDATE ProductOption po SET po.isDeleted = true WHERE po.product.id = :productId AND po.isDeleted = false")
    void markAllAsDeletedByProductId(@Param("productId") Long productId);

    List<ProductOption> findByProductId(Long productId);

    @Query("SELECT po FROM ProductOption po WHERE po.id IN :optionIds AND po.product.id = :productId")
    List<ProductOption> findAllByIdInAndProductId(@Param("optionIds") List<Long> optionIds,
                                                  @Param("productId") Long productId);

    // 배치 소프트 삭제 (핵심 최적화)
    @Modifying
    @Query("UPDATE ProductOption po SET po.isDeleted = true WHERE po.id IN :optionIds AND po.product.id = :productId AND po.isDeleted = false")
    int softDeleteByIds(@Param("optionIds") List<Long> optionIds, @Param("productId") Long productId);

    // 활성 옵션 개수 조회 (검증용)
    @Query("SELECT COUNT(po) FROM ProductOption po WHERE po.product.id = :productId AND po.isDeleted = false AND po.id NOT IN :excludeIds")
    int countActiveOptionsExcluding(@Param("productId") Long productId, @Param("excludeIds") List<Long> excludeIds);
}
