package com.southstand.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ContentRelationRequest {

    @NotBlank
    private String relationType;

    @NotNull
    private Long relationId;

    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public Long getRelationId() { return relationId; }
    public void setRelationId(Long relationId) { this.relationId = relationId; }
}
