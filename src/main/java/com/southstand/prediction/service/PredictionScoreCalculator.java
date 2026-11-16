package com.southstand.prediction.service;

/**
 * 预测洞察分与看台挑战结算规则。
 *
 * <p>Brier 分数 B = Σ(p_i - y_i)^2；洞察分 S = round(100 × (1 - B/2))。</p>
 */
public final class PredictionScoreCalculator {

    public static final String HOME = "HOME";
    public static final String DRAW = "DRAW";
    public static final String AWAY = "AWAY";

    private PredictionScoreCalculator() {
    }

    public static String outcomeOf(int homeProbability, int drawProbability, int awayProbability) {
        int max = Math.max(homeProbability, Math.max(drawProbability, awayProbability));
        if (homeProbability == max) {
            return HOME;
        }
        if (drawProbability == max) {
            return DRAW;
        }
        return AWAY;
    }

    public static String actualOutcome(Integer homeScore, Integer awayScore) {
        if (homeScore == null || awayScore == null) {
            return null;
        }
        if (homeScore > awayScore) {
            return HOME;
        }
        if (homeScore < awayScore) {
            return AWAY;
        }
        return DRAW;
    }

    public static int score(int homeProbability, int drawProbability, int awayProbability, String actualOutcome) {
        double brier = Math.pow(homeProbability / 100.0 - (HOME.equals(actualOutcome) ? 1 : 0), 2)
                + Math.pow(drawProbability / 100.0 - (DRAW.equals(actualOutcome) ? 1 : 0), 2)
                + Math.pow(awayProbability / 100.0 - (AWAY.equals(actualOutcome) ? 1 : 0), 2);
        int value = (int) Math.round(100 * (1 - brier / 2));
        return Math.max(0, Math.min(100, value));
    }

    /**
     * 挑战激励比例 R = min(0.8, max(0, (S - 60) / 50))，奖励向下取整到整豆。
     */
    public static int reward(int stake, int score) {
        if (stake <= 0) {
            return 0;
        }
        double rate = Math.min(0.8, Math.max(0, (score - 60) / 50.0));
        return (int) Math.floor(stake * rate);
    }

    public static String calibrationLabel(Integer score) {
        if (score == null) {
            return "待结算";
        }
        if (score >= 85) {
            return "校准良好";
        }
        if (score >= 70) {
            return "基本准确";
        }
        return "仍需校准";
    }

    public static String outcomeLabel(String outcome, String homeName, String awayName) {
        return switch (outcome == null ? "" : outcome) {
            case HOME -> homeName + "胜";
            case AWAY -> awayName + "胜";
            case DRAW -> "平局";
            default -> "未知";
        };
    }
}
