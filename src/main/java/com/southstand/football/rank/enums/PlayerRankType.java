package com.southstand.football.rank.enums;

import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import java.util.Locale;

public enum PlayerRankType {
    GOALS("goals", false), ASSISTS("assists", false), YELLOW_CARDS("yellow_cards", false),
    RED_CARDS("red_cards", false), SHOTS("shots", false), SHOTS_ON_TARGET("shots_on_target", false),
    RATING("rating", false), SAVES("saves", false), APPEARANCES("appearances", false), MINUTES("minutes", false);

    private final String column;
    private final boolean ascending;
    PlayerRankType(String column, boolean ascending) { this.column=column; this.ascending=ascending; }
    public String column() { return column; }
    public boolean ascending() { return ascending; }
    public static PlayerRankType parse(String value) {
        try { return valueOf(value == null ? "" : value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new BusinessException(ErrorCode.PARAM_ERROR, "invalid player rankType"); }
    }
}
