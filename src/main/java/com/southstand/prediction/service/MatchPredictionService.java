package com.southstand.prediction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.football.league.entity.FootballLeague;
import com.southstand.football.league.mapper.FootballLeagueMapper;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.prediction.dto.SubmitPredictionRequest;
import com.southstand.prediction.entity.MatchPrediction;
import com.southstand.prediction.entity.MatchPredictionConsensus;
import com.southstand.prediction.entity.PredictionBeanLog;
import com.southstand.prediction.entity.PredictionMarket;
import com.southstand.prediction.entity.PredictionTrajectory;
import com.southstand.prediction.entity.PredictionWallet;
import com.southstand.prediction.mapper.MatchPredictionConsensusMapper;
import com.southstand.prediction.mapper.MatchPredictionMapper;
import com.southstand.prediction.mapper.PredictionBeanLogMapper;
import com.southstand.prediction.mapper.PredictionMarketMapper;
import com.southstand.prediction.mapper.PredictionTrajectoryMapper;
import com.southstand.prediction.mapper.PredictionWalletMapper;
import com.southstand.prediction.vo.MatchPredictionVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchPredictionService {

    private static final Set<Integer> ALLOWED_STAKES = Set.of(0, 10, 30, 50);
    private static final List<Integer> STAKE_OPTIONS = List.of(10, 30, 50);

    private final MatchPredictionMapper predictionMapper;
    private final MatchPredictionConsensusMapper consensusMapper;
    private final PredictionWalletMapper walletMapper;
    private final PredictionWalletService walletService;
    private final PredictionBeanLogMapper beanLogMapper;
    private final PredictionTrajectoryMapper trajectoryMapper;
    private final PredictionMarketMapper marketMapper;
    private final MatchInfoMapper matchInfoMapper;
    private final FootballTeamMapper teamMapper;
    private final FootballLeagueMapper leagueMapper;
    private final ObjectMapper objectMapper;

    public MatchPredictionService(
            MatchPredictionMapper predictionMapper,
            MatchPredictionConsensusMapper consensusMapper,
            PredictionWalletMapper walletMapper,
            PredictionWalletService walletService,
            PredictionBeanLogMapper beanLogMapper,
            PredictionTrajectoryMapper trajectoryMapper,
            PredictionMarketMapper marketMapper,
            MatchInfoMapper matchInfoMapper,
            FootballTeamMapper teamMapper,
            FootballLeagueMapper leagueMapper,
            ObjectMapper objectMapper
    ) {
        this.predictionMapper = predictionMapper;
        this.consensusMapper = consensusMapper;
        this.walletMapper = walletMapper;
        this.walletService = walletService;
        this.beanLogMapper = beanLogMapper;
        this.trajectoryMapper = trajectoryMapper;
        this.marketMapper = marketMapper;
        this.matchInfoMapper = matchInfoMapper;
        this.teamMapper = teamMapper;
        this.leagueMapper = leagueMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MatchPredictionVO detail(long matchId) {
        MatchInfo match = requireMatch(matchId);
        Long userId = currentUserId();
        MatchPrediction prediction = userId == null ? null : findPrediction(matchId, userId);
        if (prediction != null) {
            settleIfPossible(match, prediction);
        }
        return buildVO(match, prediction, userId);
    }

    @Transactional
    public MatchPredictionVO submit(long matchId, SubmitPredictionRequest request) {
        Long userId = requireUserId();
        MatchInfo match = requireMatch(matchId);
        requirePredictable(match);
        if (findPrediction(matchId, userId) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "每场比赛只能提交一次判断");
        }
        int home = request.homeProbability();
        int draw = request.drawProbability();
        int away = request.awayProbability();
        if (home + draw + away != 100) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "三项概率合计必须为 100%");
        }
        int stake = request.stake() == null ? 0 : request.stake();
        if (!ALLOWED_STAKES.contains(stake)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "看台挑战仅支持 10 / 30 / 50 豆或不参与");
        }
        PredictionWallet wallet = walletService.ensureWallet(userId, "首次参与预测赠送 1,000 豆");
        if (stake > wallet.getBalance()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "看台豆余额不足");
        }
        if (stake > 0) {
            wallet.setBalance(wallet.getBalance() - stake);
            walletMapper.updateById(wallet);
            writeBeanLog(userId, null, -stake, wallet.getBalance(), "CHALLENGE_STAKE", "看台挑战投入 " + stake + " 豆");
        }

        MatchPrediction prediction = new MatchPrediction();
        prediction.setMatchId(matchId);
        prediction.setUserId(userId);
        prediction.setHomeProbability(home);
        prediction.setDrawProbability(draw);
        prediction.setAwayProbability(away);
        prediction.setPredictedOutcome(PredictionScoreCalculator.outcomeOf(home, draw, away));
        prediction.setStake(stake);
        prediction.setStatus("SUBMITTED");
        prediction.setSubmittedAt(LocalDateTime.now());
        predictionMapper.insert(prediction);
        if (stake > 0) {
            writeBeanLog(userId, prediction.getId(), 0, wallet.getBalance(), "CHALLENGE_LOCK",
                    "挑战豆已锁定，结算后返还或扣除");
        }
        writeTrajectory(matchId, userId, "提交判断", home, draw, away, 10);
        updateConsensus(matchId, home, draw, away);
        return buildVO(match, predictionMapper.selectById(prediction.getId()), userId);
    }

    private MatchPredictionVO buildVO(MatchInfo match, MatchPrediction prediction, Long userId) {
        FootballTeam homeTeam = teamMapper.selectById(match.getHomeTeamId());
        FootballTeam awayTeam = teamMapper.selectById(match.getAwayTeamId());
        FootballLeague league = match.getLeagueId() == null ? null : leagueMapper.selectById(match.getLeagueId());
        String homeName = homeTeam == null ? "主队" : homeTeam.getTeamName();
        String awayName = awayTeam == null ? "客队" : awayTeam.getTeamName();

        MatchPredictionConsensus consensus = ensureConsensus(match.getId());
        int homePercent = percent(consensus.getHomePercent(), 33);
        int drawPercent = percent(consensus.getDrawPercent(), 33);
        int awayPercent = percent(consensus.getAwayPercent(), 34);

        boolean finished = "FINISHED".equals(match.getMatchStatus());
        boolean live = "LIVE".equals(match.getMatchStatus());
        boolean canceled = "CANCELED".equals(match.getMatchStatus()) || "POSTPONED".equals(match.getMatchStatus());
        boolean started = match.getMatchTime() != null && !match.getMatchTime().isAfter(LocalDateTime.now());
        boolean preMatch = !finished && !live && !canceled && !started;
        boolean canSubmit = userId != null && prediction == null && preMatch;

        String lockReason = null;
        if (canceled) {
            lockReason = "比赛已取消或延期";
        } else if (finished) {
            lockReason = "比赛已结束";
        } else if (live || started) {
            lockReason = "比赛已开始，预测窗口已锁定";
        } else if (userId == null) {
            lockReason = "登录后可参与预测";
        } else if (prediction != null) {
            lockReason = "本场判断已提交";
        }

        Integer balance = userId == null ? null
                : walletService.ensureWallet(userId, "首次参与预测赠送 1,000 豆").getBalance();

        MatchPredictionVO.MyPrediction mine = prediction == null ? null : new MatchPredictionVO.MyPrediction(
                prediction.getId(),
                prediction.getHomeProbability(),
                prediction.getDrawProbability(),
                prediction.getAwayProbability(),
                prediction.getPredictedOutcome(),
                PredictionScoreCalculator.outcomeLabel(prediction.getPredictedOutcome(), homeName, awayName),
                prediction.getInsightScore(),
                prediction.getStake() == null ? 0 : prediction.getStake(),
                prediction.getChallengeResult(),
                prediction.getRewardBeans(),
                prediction.getSubmittedAt(),
                prediction.getSettledAt()
        );

        MatchPredictionVO.Challenge challenge = new MatchPredictionVO.Challenge(
                canSubmit,
                balance,
                STAKE_OPTIONS,
                10,
                50,
                "猜中送豆并返还，失败扣除；每场仅可参与一次，开场锁定，不涉及现金。"
        );

        MatchPredictionVO.PostMatch postMatch = null;
        if (finished && prediction != null) {
            String actualOutcome = PredictionScoreCalculator.actualOutcome(match.getHomeScore(), match.getAwayScore());
            boolean hit = actualOutcome != null && actualOutcome.equals(prediction.getPredictedOutcome());
            Integer score = prediction.getInsightScore();
            if (score == null && actualOutcome != null) {
                score = PredictionScoreCalculator.score(prediction.getHomeProbability(),
                        prediction.getDrawProbability(), prediction.getAwayProbability(), actualOutcome);
            }
            String headline = readExtraText(prediction.getExtraJson(), "headline",
                    hit ? "你的最高概率结果与赛果一致" : "方向未命中，复盘一次判断");
            List<String> highlights = readExtraList(prediction.getExtraJson(), "highlights");
            if (highlights.isEmpty()) {
                highlights = defaultHighlights(hit, homeName, awayName, prediction);
            }
            postMatch = new MatchPredictionVO.PostMatch(
                    score,
                    PredictionScoreCalculator.calibrationLabel(score),
                    hit,
                    prediction.getRewardBeans(),
                    balance,
                    headline,
                    actualOutcome,
                    PredictionScoreCalculator.outcomeLabel(actualOutcome, homeName, awayName),
                    highlights
            );
        }

        List<MatchPredictionVO.TrajectoryPoint> trajectory = new ArrayList<>();
        if (prediction != null) {
            List<PredictionTrajectory> points = trajectoryMapper.selectList(
                    Wrappers.<PredictionTrajectory>lambdaQuery()
                            .eq(PredictionTrajectory::getMatchId, match.getId())
                            .eq(PredictionTrajectory::getUserId, prediction.getUserId())
                            .orderByAsc(PredictionTrajectory::getSortOrder));
            for (PredictionTrajectory point : points) {
                trajectory.add(new MatchPredictionVO.TrajectoryPoint(
                        point.getStageLabel(),
                        point.getHomePercent(),
                        point.getDrawPercent(),
                        point.getAwayPercent(),
                        PredictionScoreCalculator.outcomeLabel(
                                PredictionScoreCalculator.outcomeOf(point.getHomePercent(), point.getDrawPercent(), point.getAwayPercent()),
                                homeName, awayName)
                ));
            }
        }

        List<MatchPredictionVO.Market> markets = new ArrayList<>();
        for (PredictionMarket market : marketMapper.selectList(
                Wrappers.<PredictionMarket>lambdaQuery()
                        .eq(PredictionMarket::getMatchId, match.getId())
                        .eq(PredictionMarket::getStatus, "ACTIVE")
                        .orderByAsc(PredictionMarket::getSortOrder))) {
            markets.add(new MatchPredictionVO.Market(market.getLabel(), market.getParticipantCount(), market.getPercent()));
        }

        String window = finished ? "POST_MATCH" : (live || started) ? "LIVE" : "PRE_MATCH";
        if (canceled) {
            window = "CANCELED";
        }

        return new MatchPredictionVO(
                match.getId(),
                match.getMatchStatus(),
                window,
                canSubmit,
                lockReason,
                league == null ? null : league.getLeagueName(),
                match.getMatchTime(),
                match.getHomeScore(),
                match.getAwayScore(),
                new MatchPredictionVO.TeamSide(homeTeam == null ? 0 : homeTeam.getId(), homeName,
                        homeTeam == null ? null : homeTeam.getShortName(), homeTeam == null ? null : homeTeam.getLogoUrl()),
                new MatchPredictionVO.TeamSide(awayTeam == null ? 0 : awayTeam.getId(), awayName,
                        awayTeam == null ? null : awayTeam.getShortName(), awayTeam == null ? null : awayTeam.getLogoUrl()),
                new MatchPredictionVO.Consensus(homePercent, drawPercent, awayPercent,
                        consensus.getParticipantCount() == null ? 0 : consensus.getParticipantCount()),
                mine,
                challenge,
                postMatch,
                trajectory,
                markets,
                "娱乐性质竞猜，不涉及现金。看台豆仅用于站内挑战与数字成就。"
        );
    }

    private void requirePredictable(MatchInfo match) {
        if ("FINISHED".equals(match.getMatchStatus()) || "LIVE".equals(match.getMatchStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "比赛已开始或结束，无法提交判断");
        }
        if ("CANCELED".equals(match.getMatchStatus()) || "POSTPONED".equals(match.getMatchStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "比赛已取消或延期");
        }
        if (match.getMatchTime() != null && !match.getMatchTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.CONFLICT, "已到开赛时间，预测窗口已锁定");
        }
    }

    private MatchInfo requireMatch(long matchId) {
        MatchInfo match = matchInfoMapper.selectById(matchId);
        if (match == null || Integer.valueOf(1).equals(match.getIsDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "比赛不存在");
        }
        return match;
    }

    private MatchPrediction findPrediction(long matchId, long userId) {
        return predictionMapper.selectOne(Wrappers.<MatchPrediction>lambdaQuery()
                .eq(MatchPrediction::getMatchId, matchId)
                .eq(MatchPrediction::getUserId, userId));
    }

    private MatchPredictionConsensus ensureConsensus(long matchId) {
        MatchPredictionConsensus consensus = consensusMapper.selectById(matchId);
        if (consensus != null) {
            return consensus;
        }
        consensus = new MatchPredictionConsensus();
        consensus.setMatchId(matchId);
        consensus.setHomePercent(new BigDecimal("33"));
        consensus.setDrawPercent(new BigDecimal("33"));
        consensus.setAwayPercent(new BigDecimal("34"));
        consensus.setParticipantCount(0);
        consensusMapper.insert(consensus);
        return consensus;
    }

    private void updateConsensus(long matchId, int home, int draw, int away) {
        MatchPredictionConsensus consensus = ensureConsensus(matchId);
        int count = consensus.getParticipantCount() == null ? 0 : consensus.getParticipantCount();
        int nextCount = count + 1;
        BigDecimal oldHome = consensus.getHomePercent() == null ? BigDecimal.ZERO : consensus.getHomePercent();
        BigDecimal oldDraw = consensus.getDrawPercent() == null ? BigDecimal.ZERO : consensus.getDrawPercent();
        BigDecimal oldAway = consensus.getAwayPercent() == null ? BigDecimal.ZERO : consensus.getAwayPercent();
        BigDecimal homePercent = oldHome.multiply(BigDecimal.valueOf(count))
                .add(BigDecimal.valueOf(home)).divide(BigDecimal.valueOf(nextCount), 2, RoundingMode.HALF_UP);
        BigDecimal drawPercent = oldDraw.multiply(BigDecimal.valueOf(count))
                .add(BigDecimal.valueOf(draw)).divide(BigDecimal.valueOf(nextCount), 2, RoundingMode.HALF_UP);
        BigDecimal awayPercent = oldAway.multiply(BigDecimal.valueOf(count))
                .add(BigDecimal.valueOf(away)).divide(BigDecimal.valueOf(nextCount), 2, RoundingMode.HALF_UP);
        int h = homePercent.setScale(0, RoundingMode.HALF_UP).intValue();
        int d = drawPercent.setScale(0, RoundingMode.HALF_UP).intValue();
        int a = awayPercent.setScale(0, RoundingMode.HALF_UP).intValue();
        int diff = 100 - (h + d + a);
        if (diff != 0) {
            if (h >= d && h >= a) {
                h += diff;
            } else if (d >= a) {
                d += diff;
            } else {
                a += diff;
            }
        }
        consensus.setHomePercent(BigDecimal.valueOf(h));
        consensus.setDrawPercent(BigDecimal.valueOf(d));
        consensus.setAwayPercent(BigDecimal.valueOf(a));
        consensus.setParticipantCount(nextCount);
        consensusMapper.updateById(consensus);
    }

    private void settleIfPossible(MatchInfo match, MatchPrediction prediction) {
        if (!"SUBMITTED".equals(prediction.getStatus())) {
            return;
        }
        if ("CANCELED".equals(match.getMatchStatus()) || "POSTPONED".equals(match.getMatchStatus())) {
            if (prediction.getStake() != null && prediction.getStake() > 0) {
                credit(prediction.getUserId(), prediction.getId(), prediction.getStake(), "CHALLENGE_REFUND", "比赛取消，返还挑战豆");
            }
            prediction.setStatus("CANCELED");
            prediction.setSettledAt(LocalDateTime.now());
            predictionMapper.updateById(prediction);
            return;
        }
        if (!"FINISHED".equals(match.getMatchStatus())) {
            return;
        }
        String actual = PredictionScoreCalculator.actualOutcome(match.getHomeScore(), match.getAwayScore());
        if (actual == null) {
            return;
        }
        int score = PredictionScoreCalculator.score(prediction.getHomeProbability(),
                prediction.getDrawProbability(), prediction.getAwayProbability(), actual);
        boolean hit = actual.equals(prediction.getPredictedOutcome());
        int stake = prediction.getStake() == null ? 0 : prediction.getStake();
        prediction.setInsightScore(score);
        prediction.setStatus("SETTLED");
        prediction.setSettledAt(LocalDateTime.now());
        if (stake > 0) {
            if (hit) {
                int bonus = PredictionScoreCalculator.reward(stake, score);
                credit(prediction.getUserId(), prediction.getId(), stake + bonus, "CHALLENGE_REWARD",
                        "方向命中，返还 " + stake + " 豆并奖励 " + bonus + " 豆");
                prediction.setChallengeResult("HIT");
                prediction.setRewardBeans(bonus);
            } else {
                prediction.setChallengeResult("MISS");
                prediction.setRewardBeans(0);
            }
        } else {
            prediction.setChallengeResult("NONE");
            prediction.setRewardBeans(0);
        }
        predictionMapper.updateById(prediction);
    }

    private void credit(long userId, long predictionId, int amount, String type, String remark) {
        PredictionWallet wallet = walletService.ensureWallet(userId, "首次参与预测赠送 1,000 豆");
        wallet.setBalance(wallet.getBalance() + amount);
        walletMapper.updateById(wallet);
        writeBeanLog(userId, predictionId, amount, wallet.getBalance(), type, remark);
    }

    private void writeBeanLog(long userId, Long predictionId, int change, int balance, String type, String remark) {
        PredictionBeanLog log = new PredictionBeanLog();
        log.setUserId(userId);
        log.setPredictionId(predictionId);
        log.setChangeAmount(change);
        log.setBalanceAfter(balance);
        log.setLogType(type);
        log.setRemark(remark);
        beanLogMapper.insert(log);
    }

    private void writeTrajectory(long matchId, long userId, String label, int home, int draw, int away, int order) {
        PredictionTrajectory point = new PredictionTrajectory();
        point.setMatchId(matchId);
        point.setUserId(userId);
        point.setStageLabel(label);
        point.setHomePercent(home);
        point.setDrawPercent(draw);
        point.setAwayPercent(away);
        point.setSortOrder(order);
        point.setCapturedAt(LocalDateTime.now());
        trajectoryMapper.insert(point);
    }

    private List<String> defaultHighlights(boolean hit, String homeName, String awayName, MatchPrediction prediction) {
        List<String> values = new ArrayList<>();
        values.add(hit ? "方向命中：你的最高概率结果与赛果一致" : "方向未命中：保留判断，下一场继续校准");
        values.add("你的判断已计入球迷共识");
        values.add("赛后复盘会保留完整概率轨迹");
        return values;
    }

    private String readExtraText(String json, String field, String fallback) {
        try {
            if (json == null || json.isBlank()) {
                return fallback;
            }
            JsonNode node = objectMapper.readTree(json);
            JsonNode value = node.get(field);
            return value == null || value.isNull() ? fallback : value.asText(fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private List<String> readExtraList(String json, String field) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            JsonNode node = objectMapper.readTree(json).get(field);
            if (node == null || !node.isArray()) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            node.forEach(item -> values.add(item.asText()));
            return values;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private int percent(BigDecimal value, int fallback) {
        return value == null ? fallback : value.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUserContext context) {
            return context.getUserId();
        }
        return null;
    }

    private Long requireUserId() {
        Long userId = currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
