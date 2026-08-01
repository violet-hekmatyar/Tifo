package com.southstand.football.rank.enums;

import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import java.util.Locale;

public enum TeamRankType {
    GOALS_FOR("goals_for", false), GOALS_AGAINST("goals_against", true), ASSISTS("assists", false),
    YELLOW_CARDS("yellow_cards", false), RED_CARDS("red_cards", false), SHOTS("shots", false),
    SHOTS_ON_TARGET("shots_on_target", false), CORNERS("corners", false), FOULS("fouls", false),
    CLEAN_SHEETS("clean_sheets", false), AVG_RATING("avg_rating", false);

    private final String column;
    private final boolean ascending;
    TeamRankType(String column, boolean ascending) { this.column=column; this.ascending=ascending; }
    public String column() { return column; }
    public boolean ascending() { return ascending; }
    public String direction() { return ascending ? "ASC" : "DESC"; }
    public static TeamRankType parse(String value) {
        try { return valueOf(value == null ? "" : value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new BusinessException(ErrorCode.PARAM_ERROR, "invalid team rankType"); }
    }
}
