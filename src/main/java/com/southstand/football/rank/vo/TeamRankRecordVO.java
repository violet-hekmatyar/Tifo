package com.southstand.football.rank.vo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record TeamRankRecordVO(Integer rank, Long teamId, String teamName, String teamLogoUrl,
        BigDecimal value, String displayValue, Integer played, String sortDirection, LocalDateTime updatedAt) {}
