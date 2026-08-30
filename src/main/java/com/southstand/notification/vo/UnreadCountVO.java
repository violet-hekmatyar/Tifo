package com.southstand.notification.vo;

import java.util.Map;

public record UnreadCountVO(long total, Map<String, Long> byType) {}
