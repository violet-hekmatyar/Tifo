package com.southstand.notification.vo;

import java.time.LocalDateTime;

public record NotificationVO(Long notificationId, String notificationType, Actor actor,
        String targetType, Long targetId, String secondaryTargetType, Long secondaryTargetId,
        String title, String content, boolean read, LocalDateTime readTime, LocalDateTime createTime,
        boolean targetAvailable, TargetPreview targetPreview) {
    public record Actor(Long userId, String nickname, String avatarUrl) {}
    public record TargetPreview(String contentTitle, String coverUrl, String commentExcerpt) {}
}
