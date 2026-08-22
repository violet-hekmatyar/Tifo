package com.southstand.recommend.model;

public enum RecommendationReasonCode {
    MAIN_TEAM("与你的主队有关"),
    FOLLOWED_AUTHOR("你关注的作者"),
    FOLLOWED_TEAM("你关注的球队"),
    FOLLOWED_PLAYER("你关注的球员"),
    SIMILAR_CONTENT("与你最近看过的内容相似"),
    LIVE_MATCH("正在直播"),
    IMPORTANT_MATCH("焦点比赛"),
    UPCOMING_MATCH("即将开始"),
    FRESH_CONTENT("新鲜内容"),
    TRENDING("热门内容");

    private final String displayText;

    RecommendationReasonCode(String displayText) {
        this.displayText = displayText;
    }

    public String getDisplayText() {
        return displayText;
    }
}

