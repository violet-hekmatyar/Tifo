package com.southstand.football.rank.vo;
import java.time.LocalDate;
public record SeasonVO(Long seasonId, Long leagueId, String seasonCode, String seasonName,
                       LocalDate startDate, LocalDate endDate, boolean current, String status) {}
