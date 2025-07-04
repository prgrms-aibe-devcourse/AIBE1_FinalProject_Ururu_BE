package com.ururulab.ururu.review.domain.entity;

import static com.ururulab.ururu.review.domain.policy.ReviewPolicy.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.global.domain.entity.Tag;
import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.global.domain.entity.enumerated.SkinType;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.review.domain.entity.enumerated.AgeGroup;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;


	// TODO: nullable = false 추가
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

	// TODO: nullable = false 추가
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(nullable = false)
	private Long productOptionId;

	@Min(RATING_MIN)
	@Max(RATING_MAX)
	@Column(nullable = false)
	private Integer rating;

	@Enumerated(EnumType.STRING)
	private SkinType skinType;

	@Enumerated(EnumType.STRING)
	private AgeGroup ageGroup;

	@Enumerated(EnumType.STRING)
	private Gender gender;

	@Column(length = CONTENT_MAX_LENGTH)
	private String content;

	@Column(nullable = false)
	private Boolean isDelete = false;

	@Column(nullable = false)
	private Integer likeCount = 0;

	@OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ReviewTag> reviewTags = new ArrayList<>();

	@OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ReviewImage> reviewImages = new ArrayList<>();

	public static Review ofCreate(
			// Member member,
			// Product product,
			Long productOptionId,
			Integer rating,
			SkinType skinType,
			AgeGroup ageGroup,
			Gender gender,
			String content
			// List<Tag> tags
	) {
		validateRating(rating);
		validateContent(content);

		Review review = new Review();
		// review.member = validateMember(member);
		// review.product = validateProduct(product);
		review.productOptionId = validateProductOptionId(productOptionId);
		review.rating = rating;
		review.skinType = skinType;
		review.ageGroup = ageGroup;
		review.gender = gender;
		review.content = content;

		// addReviewTag(review, tags);
		return review;
	}

	// TODO: CustomException
	public void addImages(List<String> imageUrls) {
		for (int i = 0; i < imageUrls.size(); i++) {
			reviewImages.add(
					ReviewImage.ofAdd(this, imageUrls.get(i), i)
			);
		}
	}

	private static Member validateMember(Member member) {
		if (member == null) {
			throw new IllegalArgumentException("Member는 필수입니다.");
		}
		return member;
	}

	private static Product validateProduct(Product product) {
		if (product == null) {
			throw new IllegalArgumentException("Product는 필수입니다.");
		}
		return product;
	}

	private static Long validateProductOptionId(Long productOptionId) {
		if (productOptionId == null) {
			throw new IllegalArgumentException("상품 옵션 ID는 필수입니다.");
		}
		return productOptionId;
	}

	private static void validateRating(Integer rating) {
		if (rating == null || rating < RATING_MIN || rating > RATING_MAX) {
			throw new IllegalArgumentException(
					"평점은 " + RATING_MIN + "부터 " + RATING_MAX + " 사이여야 합니다."
			);
		}
	}

	private static void validateContent(String content) {
		if (content != null && content.length() > CONTENT_MAX_LENGTH) {
			throw new IllegalArgumentException(
					"리뷰 내용은 최대 " + CONTENT_MAX_LENGTH + "자까지 허용됩니다."
			);
		}
	}

	private static void addReviewTag(Review review, List<Tag> tags) {
		review.reviewTags = new LinkedList<>();
		tags.forEach(
				tag -> review.reviewTags.add(
						ReviewTag.ofAdd(
								review,
								tag
						)
				)
		);
	}

}
