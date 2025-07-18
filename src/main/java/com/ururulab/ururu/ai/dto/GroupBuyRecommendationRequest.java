package com.ururulab.ururu.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GroupBuyRecommendationRequest(
    @Valid @NotNull(message = "뷰티 프로필은 필수입니다")
    BeautyProfile beautyProfile,
    
    @Min(value = 1, message = "추천 개수는 1개 이상이어야 합니다")
    @Max(value = 100, message = "추천 개수는 100개 이하여야 합니다")
    Integer topK,
    
    @Min(value = 10, message = "최소 가격은 10원 이상이어야 합니다")
    Integer minPrice,
    
    @Max(value = 10000000, message = "최대 가격은 1000만원 이하여야 합니다")
    Integer maxPrice,
    
    String additionalInfo,
    
    List<String> interestCategories,
    
    @Min(value = 0, message = "유사도는 0 이상이어야 합니다")
    @Max(value = 1, message = "유사도는 1 이하여야 합니다")
    Double minSimilarity,
    
    Boolean usePriceFilter
) {
    public record BeautyProfile(
        @NotNull(message = "피부 타입은 필수입니다")
        String skinType,
        
        String skinTone,
        
        List<String> concerns,
        
        Boolean hasAllergy,
        
        List<String> allergies,
        
        List<String> interestCategories
    ) {}
    
    @AssertTrue(message = "최소 가격은 최대 가격보다 작거나 같아야 합니다")
    public boolean isValidPriceRange() {
        return minPrice == null || maxPrice == null || minPrice <= maxPrice;
    }
}
