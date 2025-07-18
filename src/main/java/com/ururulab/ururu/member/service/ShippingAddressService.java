package com.ururulab.ururu.member.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.ShippingAddress;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.member.domain.repository.ShippingAddressRepository;
import com.ururulab.ururu.member.dto.request.ShippingAddressRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingAddressService {
    private final ShippingAddressRepository shippingAddressRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ShippingAddress createShippingAddress(Long memberId, ShippingAddressRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_EXIST));

        int currentCount = shippingAddressRepository.countByMemberId(memberId);
        if (currentCount >= 5) {
            throw new BusinessException(ErrorCode.SHIPPING_ADDRESS_LIMIT_EXCEEDED);
        }

        if (request.isDefault()) {
            unsetExistingDefaultAddress(member);
        }

        ShippingAddress shippingAddress = ShippingAddress.of(
                member,
                request.label(),
                request.phone(),
                request.zonecode(),
                request.address1(),
                request.address2(),
                request.isDefault()
        );

        ShippingAddress savedAddress = shippingAddressRepository.save(shippingAddress);
        log.debug("ShippingAddress created for member ID: {}", memberId);
        return savedAddress;
    }

    @Transactional(readOnly = true)
    public List<ShippingAddress> getShippingAddresses(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_EXIST));
        return shippingAddressRepository.findByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    public ShippingAddress getShippingAddressById(Long memberId, Long addressId) {
        return shippingAddressRepository.findByIdAndMemberId(addressId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPPING_ADDRESS_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ShippingAddress getDefaultShippingAddress(Long memberId) {
        return shippingAddressRepository.findByMemberIdAndIsDefaultTrue(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEFAULT_SHIPPING_ADDRESS_NOT_FOUND));
    }

    @Transactional
    public ShippingAddress updateShippingAddress(
            Long memberId,
            Long addressId,
            ShippingAddressRequest request
    ) {
        ShippingAddress shippingAddress = shippingAddressRepository.findByIdAndMemberId(addressId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPPING_ADDRESS_NOT_FOUND));

        if (request.isDefault() && !shippingAddress.isDefault()) {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_EXIST));

            unsetExistingDefaultAddress(member);
        }

        shippingAddress.updateAddress(
                request.label(),
                request.phone(),
                request.zonecode(),
                request.address1(),
                request.address2(),
                request.isDefault()
        );

        ShippingAddress updatedAddress = shippingAddressRepository.save(shippingAddress);
        log.debug("ShippingAddress updated for member ID: {}, address ID: {}", memberId, addressId);

        return updatedAddress;
    }

    @Transactional
    public void deleteShippingAddress(Long memberId, Long addressId) {
        boolean exists = shippingAddressRepository.existsByIdAndMemberId(addressId, memberId);
        if (!exists) {
            throw new BusinessException(ErrorCode.SHIPPING_ADDRESS_NOT_FOUND);
        }

        shippingAddressRepository.deleteByIdAndMemberId(addressId, memberId);
        log.debug("ShippingAddress deleted for member ID: {}, address ID: {}", memberId, addressId);
    }

    @Transactional
    public ShippingAddress setDefaultShippingAddress(Long memberId, Long addressId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_EXIST));

        ShippingAddress shippingAddress = shippingAddressRepository.findByIdAndMemberId(addressId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPPING_ADDRESS_NOT_FOUND));

        unsetExistingDefaultAddress(member);
        shippingAddress.setAsDefault();

        ShippingAddress updatedAddress = shippingAddressRepository.save(shippingAddress);
        log.debug("Default shipping address set for member ID: {}, address ID: {}", memberId, addressId);

        return updatedAddress;
    }

    @Transactional
    public void unsetDefaultShippingAddress(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_EXIST));

        unsetExistingDefaultAddress(member);
    }

    private void unsetExistingDefaultAddress(Member member) {
        shippingAddressRepository.findByMemberAndIsDefaultTrue(member)
                .ifPresent(defaultAddress -> {
                    defaultAddress.unsetAsDefault();
                    shippingAddressRepository.save(defaultAddress);
                });
    }
}
