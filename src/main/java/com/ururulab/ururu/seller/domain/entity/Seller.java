package com.ururulab.ururu.seller.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.seller.domain.constant.SellerConstants;
import com.ururulab.ururu.seller.domain.validator.SellerValidator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "sellers", indexes = {
    @Index(name = "idx_seller_business_number", columnList = "businessNumber", unique = true),
    @Index(name = "idx_seller_name", columnList = "name", unique = true),
    @Index(name = "idx_seller_deleted_updated_at", columnList = "isDeleted, updatedAt")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seller extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = SellerConstants.NAME_MAX_LENGTH, nullable = false)
    private String name; // 브랜드명

    @Column(length = SellerConstants.BUSINESS_NAME_MAX_LENGTH, nullable = false)
    private String businessName; // 사업자명

    @Column(length = SellerConstants.OWNER_NAME_MAX_LENGTH, nullable = false)
    private String ownerName; // 대표 CEO명

    @Column(length = SellerConstants.BUSINESS_NUMBER_LENGTH, nullable = false)
    private String businessNumber; // 사업자등록번호

    @Column(length = SellerConstants.EMAIL_MAX_LENGTH, nullable = false, unique = true)
    private String email;

    @Column(length = SellerConstants.PASSWORD_COLUMN_LENGTH, nullable = false) // 암호화된 비밀번호는 고정 길이
    private String password; // 암호화된 비밀번호

    @Column(length = SellerConstants.PHONE_MAX_LENGTH, nullable = false)
    private String phone;

    @Column(length = SellerConstants.IMAGE_COLUMN_LENGTH) // 이미지 URL은 고정 길이
    private String image; // 브랜드 대표 이미지

    @Column(length = SellerConstants.ZONECODE_COLUMN_LENGTH, nullable = false)
    private String zonecode; // 우편번호

    @Column(length = SellerConstants.ADDRESS_COLUMN_LENGTH, nullable = false) // 주소는 고정 길이
    private String address1;

    @Column(length = SellerConstants.ADDRESS_COLUMN_LENGTH, nullable = false) // 주소는 고정 길이
    private String address2;

    @Column(length = SellerConstants.MAIL_ORDER_NUMBER_MAX_LENGTH, nullable = false)
    private String mailOrderNumber; // 통신판매업 신고번호

    @Column(nullable = false)
    private Boolean isDeleted = false;

    public static Seller of(
            String name,
            String businessName,
            String ownerName,
            String businessNumber,
            String email,
            String password,
            String phone,
            String image,
            String zonecode,
            String address1,
            String address2,
            String mailOrderNumber
    ) {
        // 도메인 무결성 검증
        SellerValidator.validateName(name);
        SellerValidator.validateBusinessName(businessName);
        SellerValidator.validateOwnerName(ownerName);
        SellerValidator.validateBusinessNumber(businessNumber);
        String normalizedEmail = SellerValidator.normalizeAndValidateEmail(email);
        SellerValidator.validatePhone(phone);
        SellerValidator.validateZonecode(zonecode);
        SellerValidator.validateAddress1(address1);
        SellerValidator.validateAddress2(address2);
        SellerValidator.validateMailOrderNumber(mailOrderNumber);

        Seller seller = new Seller();
        seller.name = name.trim();
        seller.businessName = businessName.trim();
        seller.ownerName = ownerName.trim();
        seller.businessNumber = businessNumber.trim();
        seller.email = normalizedEmail;
        seller.password = password; // 암호화는 Service 레이어에서 처리
        seller.phone = phone.trim();
        seller.image = image;
        seller.zonecode = zonecode.trim();
        seller.address1 = address1.trim();
        seller.address2 = address2.trim();
        seller.mailOrderNumber = mailOrderNumber.trim();
        return seller;
    }

    public void updateName(final String name) {
        SellerValidator.validateName(name);
        this.name = name.trim();
    }

    public void updateBusinessName(final String businessName) {
        SellerValidator.validateBusinessName(businessName);
        this.businessName = businessName.trim();
    }

    public void updateOwnerName(final String ownerName) {
        SellerValidator.validateOwnerName(ownerName);
        this.ownerName = ownerName.trim();
    }

    public void updateEmail(final String email) {
        String normalizedEmail = SellerValidator.normalizeAndValidateEmail(email);
        this.email = normalizedEmail;
    }

    public void updatePhone(final String phone) {
        SellerValidator.validatePhone(phone);
        this.phone = phone.trim();
    }

    public void updateImage(final String image) {
        this.image = image;
    }

    public void updateAddress(final String zonecode, final String address1, final String address2) {
        SellerValidator.validateZonecode(zonecode);
        SellerValidator.validateAddress1(address1);
        SellerValidator.validateAddress2(address2);
        this.zonecode = zonecode.trim();
        this.address1 = address1.trim();
        this.address2 = address2 != null ? address2.trim() : "";
    }

    public void updateMailOrderNumber(final String mailOrderNumber) {
        SellerValidator.validateMailOrderNumber(mailOrderNumber);
        this.mailOrderNumber = mailOrderNumber.trim();
    }

    public void delete() {
        this.isDeleted = true;
    }
}
