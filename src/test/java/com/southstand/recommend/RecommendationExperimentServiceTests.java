package com.southstand.recommend;

import static org.assertj.core.api.Assertions.assertThat;

import com.southstand.recommend.config.RecommendationProperties;
import com.southstand.recommend.service.RecommendationExperimentService;
import org.junit.jupiter.api.Test;

class RecommendationExperimentServiceTests {
    @Test
    void assignmentIsStableAndAnonymousUsesRule() {
        RecommendationProperties properties = new RecommendationProperties();
        properties.getExperiment().setCfPercent(50);
        var service = new RecommendationExperimentService(properties);
        assertThat(service.assign(42L).bucket()).isEqualTo(service.assign(42L).bucket());
        assertThat(service.assign(null).bucket()).isEqualTo("A");
        assertThat(service.assign(null).useCf()).isFalse();
    }

    @Test
    void boundaryPercentagesAreHonored() {
        RecommendationProperties properties = new RecommendationProperties();
        var service = new RecommendationExperimentService(properties);
        properties.getExperiment().setCfPercent(100);
        assertThat(service.assign(7L).bucket()).isEqualTo("B");
        properties.getExperiment().setCfPercent(0);
        assertThat(service.assign(7L).bucket()).isEqualTo("A");
    }
}
