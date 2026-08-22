package com.southstand.card.vo;

import java.util.List;

public record RankingCardPayload(String rankingType, String rankType, Long leagueId,
                                 String leagueName, Long seasonId, String seasonName,
                                 String title, List<RankingItem> items) implements HomeCardPayload {
    public record RankingItem(int rank, Long entityId, String name, String imageUrl,
                              Long teamId, String teamName, String value) { }
}
