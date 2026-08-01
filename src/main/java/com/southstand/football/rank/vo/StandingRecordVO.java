package com.southstand.football.rank.vo;
public record StandingRecordVO(Integer rank, Long teamId, String teamName, String teamLogoUrl,
        Integer played, Integer won, Integer drawn, Integer lost, Integer goalsFor, Integer goalsAgainst,
        Integer goalDifference, Integer points, Integer deductionPoints, String form) {}
