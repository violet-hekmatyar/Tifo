package com.southstand.recommend.strategy;

import com.southstand.recommend.model.RecommendationContext;
import com.southstand.recommend.model.RecommendationItem;
import java.util.List;

public interface RecommendationStrategy {
    List<RecommendationItem> rank(RecommendationContext context);
}

