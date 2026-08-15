package com.memory_atelier.recommend.domain;

// 큐레이션 추천 상품 한 개. AI job의 done 응답 curation.recommendations 항목과 1:1 대응
public record RecommendationItem(String productId, String nameKr, String reason, String tagline, String imageUrl) {
}
