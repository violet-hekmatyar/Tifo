package com.southstand.card.model;

import java.util.LinkedHashSet;
import java.util.Set;

public record HomeFeedUserContext(Long userId, Long mainTeamId, Set<Long> followedTeamIds,
                                  Set<Long> followedPlayerIds, Set<Long> followedUserIds) {
    public HomeFeedUserContext {
        followedTeamIds = copy(followedTeamIds);
        followedPlayerIds = copy(followedPlayerIds);
        followedUserIds = copy(followedUserIds);
    }

    public static HomeFeedUserContext anonymous() {
        return new HomeFeedUserContext(null, null, Set.of(), Set.of(), Set.of());
    }

    private static Set<Long> copy(Set<Long> value) {
        return value == null ? Set.of() : new LinkedHashSet<>(value);
    }
}
