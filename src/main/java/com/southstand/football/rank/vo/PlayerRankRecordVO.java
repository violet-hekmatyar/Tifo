package com.southstand.football.rank.vo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record PlayerRankRecordVO(Integer rank, Long playerId, String playerName, String playerAvatarUrl,
        Long teamId, String teamName, String teamLogoUrl, BigDecimal value, String displayValue,
        Integer appearances, Integer starts, Integer minutes, LocalDateTime updatedAt) {}
