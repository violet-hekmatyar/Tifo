package com.southstand.content.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class CreatePostRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String body;

    @Size(max = 9)
    private List<@Size(max = 512) String> mediaUrls;

    @Valid
    @Size(max = 10)
    private List<ContentRelationRequest> relationList;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public List<String> getMediaUrls() { return mediaUrls; }
    public void setMediaUrls(List<String> mediaUrls) { this.mediaUrls = mediaUrls; }
    public List<ContentRelationRequest> getRelationList() { return relationList; }
    public void setRelationList(List<ContentRelationRequest> relationList) { this.relationList = relationList; }
}
