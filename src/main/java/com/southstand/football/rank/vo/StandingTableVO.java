package com.southstand.football.rank.vo;
import java.time.LocalDateTime;
import java.util.List;
public record StandingTableVO(Long leagueId, String leagueName, Long seasonId, String seasonName,
        Long stageId, String stageName, String groupCode, String source, LocalDateTime updatedAt,
        List<StandingRecordVO> records) {}
