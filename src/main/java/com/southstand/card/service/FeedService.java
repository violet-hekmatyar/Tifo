package com.southstand.card.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.card.vo.FeedCardVO;
import com.southstand.card.vo.FeedPageResult;
import com.southstand.card.vo.HotLeagueVO;
import com.southstand.card.vo.RelationTagVO;
import com.southstand.card.model.HomeFeedUserContext;
import com.southstand.content.entity.Content;
import com.southstand.content.entity.ContentRelation;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.content.mapper.ContentRelationMapper;
import com.southstand.content.vo.AuthorVO;
import com.southstand.follow.entity.FollowRecord;
import com.southstand.follow.mapper.FollowRecordMapper;
import com.southstand.football.event.entity.MatchEvent;
import com.southstand.football.event.mapper.MatchEventMapper;
import com.southstand.football.league.entity.FootballLeague;
import com.southstand.football.league.mapper.FootballLeagueMapper;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.football.match.vo.MatchTeamVO;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.report.entity.MatchReport;
import com.southstand.football.report.mapper.MatchReportMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.interaction.entity.FavoriteRecord;
import com.southstand.interaction.entity.LikeRecord;
import com.southstand.interaction.entity.Comment;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.interaction.mapper.FavoriteRecordMapper;
import com.southstand.interaction.mapper.LikeRecordMapper;
import com.southstand.interaction.service.CommentService;
import com.southstand.interaction.vo.HotCommentVO;
import com.southstand.recommend.model.RecommendationCandidate;
import com.southstand.recommend.model.RecommendationContext;
import com.southstand.recommend.model.RecommendationItem;
import com.southstand.recommend.model.RecommendationTargetType;
import com.southstand.recommend.service.RecommendationBehaviorService;
import com.southstand.recommend.service.RecommendationService;
import com.southstand.recommend.vo.RecommendationResult;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.UserProfileMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FeedService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {};

    private static final String ACTIVE = "ACTIVE";
    private static final String PUBLISHED = "PUBLISHED";
    private static final int NOT_DELETED = 0;
    private static final String CARD_CONTENT = "CONTENT";
    private static final String CARD_MATCH = "MATCH";
    private static final String TEAM = "TEAM";
    private static final String PLAYER = "PLAYER";
    private static final String USER = "USER";
    private static final String MATCH = "MATCH";

    private final ContentMapper contentMapper;
    private final ContentRelationMapper contentRelationMapper;
    private final MatchInfoMapper matchInfoMapper;
    private final MatchEventMapper matchEventMapper;
    private final MatchReportMapper matchReportMapper;
    private final FootballLeagueMapper leagueMapper;
    private final FootballTeamMapper teamMapper;
    private final FootballPlayerMapper playerMapper;
    private final FollowRecordMapper followRecordMapper;
    private final UserProfileMapper userProfileMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final FavoriteRecordMapper favoriteRecordMapper;
    private final CommentMapper commentMapper;
    private final RecommendationService recommendationService;
    private final RecommendationBehaviorService recommendationBehaviorService;
    private final AuxiliaryCardService auxiliaryCardService;
    private final HomeFeedCompositionService compositionService;

    @Autowired
    public FeedService(
            ContentMapper contentMapper,
            ContentRelationMapper contentRelationMapper,
            MatchInfoMapper matchInfoMapper,
            MatchEventMapper matchEventMapper,
            MatchReportMapper matchReportMapper,
            FootballLeagueMapper leagueMapper,
            FootballTeamMapper teamMapper,
            FootballPlayerMapper playerMapper,
            FollowRecordMapper followRecordMapper,
            UserProfileMapper userProfileMapper,
            LikeRecordMapper likeRecordMapper,
            FavoriteRecordMapper favoriteRecordMapper,
            CommentMapper commentMapper,
            RecommendationService recommendationService,
            RecommendationBehaviorService recommendationBehaviorService,
            AuxiliaryCardService auxiliaryCardService,
            HomeFeedCompositionService compositionService
    ) {
        this.contentMapper = contentMapper;
        this.contentRelationMapper = contentRelationMapper;
        this.matchInfoMapper = matchInfoMapper;
        this.matchEventMapper = matchEventMapper;
        this.matchReportMapper = matchReportMapper;
        this.leagueMapper = leagueMapper;
        this.teamMapper = teamMapper;
        this.playerMapper = playerMapper;
        this.followRecordMapper = followRecordMapper;
        this.userProfileMapper = userProfileMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.favoriteRecordMapper = favoriteRecordMapper;
        this.commentMapper = commentMapper;
        this.recommendationService = recommendationService;
        this.recommendationBehaviorService = recommendationBehaviorService;
        this.auxiliaryCardService = auxiliaryCardService;
        this.compositionService = compositionService;
    }

    /** 保留现有测试夹具构造方式；生产环境使用完整构造器。 */
    public FeedService(
            ContentMapper contentMapper, ContentRelationMapper contentRelationMapper,
            MatchInfoMapper matchInfoMapper, MatchEventMapper matchEventMapper,
            MatchReportMapper matchReportMapper, FootballLeagueMapper leagueMapper,
            FootballTeamMapper teamMapper, FootballPlayerMapper playerMapper,
            FollowRecordMapper followRecordMapper, UserProfileMapper userProfileMapper,
            LikeRecordMapper likeRecordMapper, FavoriteRecordMapper favoriteRecordMapper,
            CommentMapper commentMapper) {
        this(contentMapper, contentRelationMapper, matchInfoMapper, matchEventMapper, matchReportMapper,
                leagueMapper, teamMapper, playerMapper, followRecordMapper, userProfileMapper,
                likeRecordMapper, favoriteRecordMapper, commentMapper, null, null, null, null);
    }

    public FeedPageResult feed(String tab, Long leagueId, Long teamId, long pageNum, long pageSize, String cursor) {
        String safeTab = StringUtils.hasText(tab) ? tab : "recommend";
        UserContext user = currentUserContext();
        List<FeedCardVO> cards = switch (safeTab) {
            case "following" -> followingCards(user, leagueId, teamId);
            case "news" -> contentCards(user, leagueId, teamId, true, false);
            case "match" -> matchCards(user, leagueId, teamId, false);
            case "mixed" -> mixedCards(user, leagueId, teamId, false);
            default -> mixedCards(user, leagueId, teamId, true);
        };
        if (recommendationService == null) {
            cards = deduplicate(cards);
            cards.sort(Comparator.comparing(FeedCardVO::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(card -> Objects.toString(card.getCardId(), "")));
            return page(cards, pageNum, pageSize);
        }
        if (cards.isEmpty() && "recommend".equals(safeTab)) {
            cards = mixedCards(user, leagueId, teamId, false);
        }
        RecommendationContext context = recommendationContext(user, safeTab, teamId, cards);
        RecommendationResult recommendation = recommendationService.recommend(context);
        List<FeedCardVO> ordered = applyRecommendation(cards, recommendation);
        if ("recommend".equals(safeTab) && auxiliaryCardService != null && compositionService != null) {
            List<FeedCardVO> auxiliary = auxiliaryCardService.candidates(homeContext(user));
            ordered = compositionService.compose(ordered, auxiliary, recommendation.getRequestId());
        }
        FeedPageResult result = page(ordered, pageNum, pageSize);
        result.setAlgorithmVersion(recommendation.getAlgorithmVersion());
        result.setModelVersion(recommendation.getModelVersion());
        result.setExperimentId(recommendation.getExperimentId());
        result.setExperimentBucket(recommendation.getExperimentBucket());
        result.setRequestId(recommendation.getRequestId());
        return result;
    }

    private RecommendationContext recommendationContext(UserContext user, String tab, Long teamId,
                                                         List<FeedCardVO> cards) {
        RecommendationContext context = new RecommendationContext();
        context.setUserId(user.userId);
        context.setMainTeamId(user.mainTeamId);
        context.setFollowedTeamIds(new HashSet<>(user.followedTeamIds));
        context.setFollowedPlayerIds(new HashSet<>(user.followedPlayerIds));
        context.setFollowedUserIds(new HashSet<>(user.followedUserIds));
        context.setScene(sceneOf(tab, teamId));
        context.setCandidates(cards.stream().map(this::recommendationCandidate).toList());
        if (recommendationBehaviorService != null && user.userId != null) {
            LocalDateTime now = LocalDateTime.now();
            context.setExposedKeys(recommendationBehaviorService.recentExposureKeys(
                    user.userId, now.minusDays(3), now.minusMinutes(5)));
        }
        return context;
    }

    private String sceneOf(String tab, Long teamId) {
        if (teamId != null) return "TEAM_FOLLOW_FEED";
        return switch (tab) {
            case "news" -> "NEWS_ONLY";
            case "following" -> "FOLLOWING_FEED";
            case "match" -> "MATCH_ONLY";
            default -> "HOME_RECOMMEND";
        };
    }

    private HomeFeedUserContext homeContext(UserContext user) {
        return new HomeFeedUserContext(user.userId, user.mainTeamId, user.followedTeamIds,
                user.followedPlayerIds, user.followedUserIds);
    }

    private RecommendationCandidate recommendationCandidate(FeedCardVO card) {
        RecommendationCandidate candidate = new RecommendationCandidate();
        candidate.setCardType(card.getCardType());
        if (CARD_MATCH.equals(card.getCardType())) {
            candidate.setTargetType(RecommendationTargetType.MATCH);
            candidate.setTargetId(card.getMatchId());
            candidate.setMatchStatus(card.getMatchStatus());
            candidate.setMatchTime(card.getMatchTime());
            candidate.setHasReport(Boolean.TRUE.equals(card.getHasReport()));
            candidate.setImportantLevel(card.getRecommendationImportantLevel());
            candidate.setHomeTeamId(card.getHomeTeam() == null ? null : card.getHomeTeam().getTeamId());
            candidate.setAwayTeamId(card.getAwayTeam() == null ? null : card.getAwayTeam().getTeamId());
            return candidate;
        }
        candidate.setTargetType(RecommendationTargetType.CONTENT);
        candidate.setTargetId(card.getContentId());
        candidate.setContentType(card.getContentType());
        candidate.setAuthorId(card.getAuthor() == null ? null : card.getAuthor().getUserId());
        candidate.setLikeCount(nvl(card.getLikeCount()));
        candidate.setCommentCount(nvl(card.getCommentCount()));
        candidate.setFavoriteCount(nvl(card.getFavoriteCount()));
        candidate.setHotScore(card.getRecommendationHotScore() == null ? 0D : card.getRecommendationHotScore());
        candidate.setPublishTime(card.getPublishTime());
        Set<Long> teamIds = new LinkedHashSet<>();
        Set<Long> playerIds = new LinkedHashSet<>();
        if (card.getRelationTags() != null) {
            for (RelationTagVO tag : card.getRelationTags()) {
                if (TEAM.equals(tag.getRelationType())) teamIds.add(tag.getRelationId());
                if (PLAYER.equals(tag.getRelationType())) playerIds.add(tag.getRelationId());
                if (MATCH.equals(tag.getRelationType()) && candidate.getRelatedMatchId() == null) {
                    candidate.setRelatedMatchId(tag.getRelationId());
                }
            }
        }
        candidate.setTeamIds(teamIds);
        candidate.setPlayerIds(playerIds);
        return candidate;
    }

    private List<FeedCardVO> applyRecommendation(List<FeedCardVO> cards, RecommendationResult result) {
        Map<String, FeedCardVO> byKey = new LinkedHashMap<>();
        for (FeedCardVO card : cards) byKey.putIfAbsent(cardKey(card), card);
        List<FeedCardVO> ordered = new ArrayList<>(cards.size());
        int position = 0;
        for (RecommendationItem item : result.getItems()) {
            FeedCardVO card = byKey.get(item.key());
            if (card == null) continue;
            card.setScore(item.getScore());
            card.setReasonCode(item.getReasonCode().name());
            card.setReason(item.getReasonCode().getDisplayText());
            card.setAlgorithmVersion(CARD_MATCH.equals(card.getCardType())
                    ? "RULE_V2" : result.getAlgorithmVersion());
            card.setPosition(position++);
            card.setImpressionId(result.getRequestId() + ":" + item.key());
            ordered.add(card);
        }
        return ordered;
    }

    private String cardKey(FeedCardVO card) {
        return CARD_MATCH.equals(card.getCardType()) ? "MATCH_" + card.getMatchId()
                : "CONTENT_" + card.getContentId();
    }

    public List<HotLeagueVO> hotLeagues(int limit) {
        int safeLimit = Math.max(1, Math.min(limit <= 0 ? 10 : limit, 20));
        UserContext user = currentUserContext();
        List<FootballLeague> leagues = leagueMapper.selectList(new QueryWrapper<FootballLeague>()
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .orderByAsc("sort_order")
                .orderByAsc("id"));
        List<HotLeagueVO> result = new ArrayList<>();
        for (FootballLeague league : leagues) {
            int live = countMatches(league.getId(), "LIVE");
            int upcoming = countMatches(league.getId(), "SCHEDULED");
            double followBoost = user.leagueIds.contains(league.getId()) ? 30 : 0;
            HotLeagueVO vo = new HotLeagueVO();
            vo.setLeagueId(league.getId());
            vo.setLeagueName(league.getLeagueName());
            vo.setLogoUrl(league.getLogoUrl());
            vo.setCountry(league.getCountry());
            vo.setLiveMatchCount(live);
            vo.setUpcomingMatchCount(upcoming);
            vo.setHotScore(20D + live * 50D + upcoming * 10D + followBoost);
            result.add(vo);
        }
        result.sort(Comparator.comparing(HotLeagueVO::getHotScore, Comparator.reverseOrder())
                .thenComparing(HotLeagueVO::getLeagueId));
        return result.size() <= safeLimit ? result : result.subList(0, safeLimit);
    }

    private List<FeedCardVO> followingCards(UserContext user, Long leagueId, Long teamId) {
        if (user.userId == null) {
            return List.of();
        }
        List<FeedCardVO> cards = new ArrayList<>();
        cards.addAll(contentCards(user, leagueId, teamId, false, true));
        cards.addAll(matchCards(user, leagueId, teamId, true));
        return cards.isEmpty() ? mixedCards(user, leagueId, teamId, false) : cards;
    }

    private List<FeedCardVO> mixedCards(UserContext user, Long leagueId, Long teamId, boolean personalized) {
        boolean personalizedOnly = personalized && user.userId != null;
        List<FeedCardVO> cards = new ArrayList<>();
        cards.addAll(contentCards(user, leagueId, teamId, false, personalizedOnly));
        cards.addAll(matchCards(user, leagueId, teamId, personalizedOnly));
        return cards;
    }

    private List<FeedCardVO> contentCards(UserContext user, Long leagueId, Long teamId, boolean newsOnly, boolean personalizedOnly) {
        QueryWrapper<Content> wrapper = new QueryWrapper<Content>()
                .eq("status", PUBLISHED)
                .eq("is_deleted", NOT_DELETED)
                .orderByDesc("hot_score")
                .orderByDesc("publish_time")
                .last("LIMIT 100");
        if (newsOnly) {
            wrapper.in("content_type", List.of("NEWS", "ARTICLE", "REPORT"));
        }
        if (personalizedOnly) {
            Set<Long> contentIds = personalizedContentIds(user, leagueId, teamId);
            if (contentIds.isEmpty()) {
                return List.of();
            }
            wrapper.in("id", contentIds);
        } else {
            Set<Long> filterIds = filteredContentIds(leagueId, teamId);
            if (!filterIds.isEmpty()) {
                wrapper.in("id", filterIds);
            }
        }
        List<Content> contents = contentMapper.selectList(wrapper);
        ContentBatch batch = loadContentBatch(contents, user.userId);
        return contents.stream()
                .map(content -> toContentCard(content, user, personalizedOnly || user.userId != null, batch))
                .collect(Collectors.toList());
    }

    private List<FeedCardVO> matchCards(UserContext user, Long leagueId, Long teamId, boolean personalizedOnly) {
        QueryWrapper<MatchInfo> wrapper = new QueryWrapper<MatchInfo>()
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .orderByDesc("important_level")
                .orderByAsc("match_time")
                .last("LIMIT 100");
        if (leagueId != null) {
            wrapper.eq("league_id", leagueId);
        }
        if (teamId != null) {
            wrapper.and(w -> w.eq("home_team_id", teamId).or().eq("away_team_id", teamId));
        } else if (personalizedOnly) {
            Set<Long> teamIds = new LinkedHashSet<>(user.followedTeamIds);
            if (user.mainTeamId != null) {
                teamIds.add(user.mainTeamId);
            }
            if (teamIds.isEmpty()) {
                return List.of();
            }
            wrapper.and(w -> w.in("home_team_id", teamIds).or().in("away_team_id", teamIds));
        }
        List<MatchInfo> matches = matchInfoMapper.selectList(wrapper);
        MatchBatch batch = loadMatchBatch(matches);
        return matches.stream()
                .map(match -> toMatchCard(match, user, batch))
                .collect(Collectors.toList());
    }

    private FeedCardVO toContentCard(Content content, UserContext user, boolean personalized, ContentBatch batch) {
        FeedCardVO card = new FeedCardVO();
        card.setCardId("CONTENT_" + content.getId());
        card.setCardKey("CONTENT:" + content.getId());
        card.setCardType(CARD_CONTENT);
        card.setContentId(content.getId());
        card.setContentType(content.getContentType());
        Map<String, Object> transferDisplay = transferDisplay(content);
        if (transferDisplay != null) {
            card.setDisplayType("TRANSFER_BRIEF");
            card.setDisplayData(Map.of("transferBrief", transferDisplay));
        }
        card.setTitle(content.getTitle());
        card.setSummary(content.getSummary());
        card.setCoverUrl(content.getCoverUrl());
        card.setAuthor(author(content.getAuthorId(), batch.profiles));
        List<ContentRelation> relations = batch.relations.getOrDefault(content.getId(), List.of());
        card.setRelationTags(relations.stream().map(relation -> relationTag(relation, user, batch)).collect(Collectors.toList()));
        card.setLikeCount(nvl(content.getLikeCount()));
        card.setCommentCount(nvl(content.getCommentCount()));
        card.setFavoriteCount(nvl(content.getFavoriteCount()));
        card.setHotComment(hotComment(content.getId(), batch));
        card.setLiked(batch.likedContentIds.contains(content.getId()));
        card.setFavorited(batch.favoritedContentIds.contains(content.getId()));
        card.setPublishTime(content.getPublishTime());
        card.setRecommendationHotScore(decimal(content.getHotScore()));
        card.setScore(contentScore(content, relations, user, personalized));
        return card;
    }

    private Map<String, Object> transferDisplay(Content content) {
        if (!"TRANSFER_BRIEF".equals(content.getCardType())
                || !StringUtils.hasText(content.getExtraJson())) {
            return null;
        }
        try {
            Map<String, Object> extra = JSON.readValue(content.getExtraJson(), JSON_OBJECT);
            if (!"TRANSFER_BRIEF".equals(extra.get("displayType"))) return null;
            Object rawBrief = extra.get("transferBrief");
            if (!(rawBrief instanceof Map<?, ?> brief)) return null;
            for (String required : List.of("playerId", "playerName", "fromTeamId", "fromTeamName", "toTeamId", "toTeamName")) {
                if (brief.get(required) == null) return null;
            }
            Map<String, Object> safe = new LinkedHashMap<>();
            for (String key : List.of(
                    "playerId", "playerName", "playerMeta", "playerAvatarUrl",
                    "fromTeamId", "fromTeamName", "fromTeamLogoUrl",
                    "toTeamId", "toTeamName", "toTeamLogoUrl", "feeLabel", "durationLabel")) {
                if (brief.containsKey(key)) safe.put(key, brief.get(key));
            }
            return safe;
        } catch (Exception ignored) {
            return null;
        }
    }

    private FeedCardVO toMatchCard(MatchInfo match, UserContext user, MatchBatch batch) {
        MatchReport report = batch.reports.get(match.getId());
        FeedCardVO card = new FeedCardVO();
        card.setCardId("MATCH_" + match.getId());
        card.setCardKey("MATCH:" + match.getId());
        card.setCardType(CARD_MATCH);
        card.setMatchId(match.getId());
        card.setLeagueId(match.getLeagueId());
        FootballLeague league = batch.leagues.get(match.getLeagueId());
        card.setLeagueName(league == null ? null : league.getLeagueName());
        card.setHomeTeam(matchTeam(batch.teams.get(match.getHomeTeamId()), match.getHomeScore()));
        card.setAwayTeam(matchTeam(batch.teams.get(match.getAwayTeamId()), match.getAwayScore()));
        card.setHomeScore(match.getHomeScore());
        card.setAwayScore(match.getAwayScore());
        card.setMatchStatus(match.getMatchStatus());
        card.setMatchTime(match.getMatchTime());
        card.setEventSummary(eventSummary(batch.events.get(match.getId()), batch.players));
        card.setHasReport(report != null || Objects.equals(match.getHasReport(), 1));
        card.setReportContentId(report == null ? null : report.getContentId());
        card.setRecommendationImportantLevel(match.getImportantLevel());
        card.setScore(matchScore(match, user));
        return card;
    }

    private double contentScore(Content content, List<ContentRelation> relations, UserContext user, boolean personalized) {
        double score = decimal(content.getHotScore())
                + nvl(content.getLikeCount()) * 2D
                + nvl(content.getCommentCount()) * 3D
                + nvl(content.getFavoriteCount()) * 4D
                + timeBoost(content.getPublishTime());
        if (personalized || user.userId != null) {
            if (content.getAuthorId() != null && user.followedUserIds.contains(content.getAuthorId())) {
                score += 45D;
            }
            for (ContentRelation relation : relations) {
                if (TEAM.equals(relation.getRelationType()) && Objects.equals(relation.getRelationId(), user.mainTeamId)) {
                    score += 50D;
                } else if (TEAM.equals(relation.getRelationType()) && user.followedTeamIds.contains(relation.getRelationId())) {
                    score += 35D;
                } else if (PLAYER.equals(relation.getRelationType()) && user.followedPlayerIds.contains(relation.getRelationId())) {
                    score += 25D;
                }
            }
        }
        return score;
    }

    private double matchScore(MatchInfo match, UserContext user) {
        double score = 0D;
        if ("LIVE".equals(match.getMatchStatus())) {
            score += 80D;
        } else if ("SCHEDULED".equals(match.getMatchStatus())) {
            score += 40D;
        } else if ("FINISHED".equals(match.getMatchStatus()) && Objects.equals(match.getHasReport(), 1)) {
            score += 25D;
        }
        if (match.getImportantLevel() != null && match.getImportantLevel() > 0) {
            score += 50D + match.getImportantLevel() * 5D;
        }
        if (Objects.equals(match.getHomeTeamId(), user.mainTeamId) || Objects.equals(match.getAwayTeamId(), user.mainTeamId)) {
            score += 50D;
        } else if (user.followedTeamIds.contains(match.getHomeTeamId()) || user.followedTeamIds.contains(match.getAwayTeamId())) {
            score += 35D;
        }
        return score;
    }

    private double timeBoost(LocalDateTime publishTime) {
        if (publishTime == null) {
            return 0D;
        }
        long hours = Duration.between(publishTime, LocalDateTime.now()).toHours();
        if (hours < 0 || hours <= 2) {
            return 30D;
        }
        if (hours <= 12) {
            return 20D;
        }
        if (hours <= 24) {
            return 10D;
        }
        return 0D;
    }

    private Set<Long> personalizedContentIds(UserContext user, Long leagueId, Long teamId) {
        Set<Long> ids = filteredContentIds(leagueId, teamId);
        Set<Long> relationIds = new LinkedHashSet<>();
        if (teamId != null) {
            relationIds.addAll(contentIdsByRelation(TEAM, Set.of(teamId)));
        } else {
            Set<Long> teamIds = new LinkedHashSet<>(user.followedTeamIds);
            if (user.mainTeamId != null) {
                teamIds.add(user.mainTeamId);
            }
            relationIds.addAll(contentIdsByRelation(TEAM, teamIds));
        }
        relationIds.addAll(contentIdsByRelation(PLAYER, user.followedPlayerIds));
        relationIds.addAll(contentIdsByAuthors(user.followedUserIds));
        if (leagueId != null) {
            relationIds.retainAll(filteredContentIds(leagueId, null));
        }
        if (!ids.isEmpty()) {
            relationIds.addAll(ids);
        }
        return relationIds;
    }

    private Set<Long> filteredContentIds(Long leagueId, Long teamId) {
        Set<Long> ids = new LinkedHashSet<>();
        if (teamId != null) {
            ids.addAll(contentIdsByRelation(TEAM, Set.of(teamId)));
        }
        if (leagueId != null) {
            List<MatchInfo> matches = matchInfoMapper.selectList(new QueryWrapper<MatchInfo>()
                    .eq("league_id", leagueId)
                    .eq("status", ACTIVE)
                    .eq("is_deleted", NOT_DELETED));
            Set<Long> matchIds = matches.stream().map(MatchInfo::getId).collect(Collectors.toCollection(LinkedHashSet::new));
            ids.addAll(contentIdsByRelation(MATCH, matchIds));
        }
        return ids;
    }

    private Set<Long> contentIdsByAuthors(Set<Long> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            return Set.of();
        }
        return contentMapper.selectList(new QueryWrapper<Content>()
                        .in("author_id", authorIds)
                        .eq("status", PUBLISHED)
                        .eq("is_deleted", NOT_DELETED)
                        .orderByDesc("publish_time")
                        .last("LIMIT 100"))
                .stream()
                .map(Content::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> contentIdsByRelation(String relationType, Set<Long> relationIds) {
        if (relationIds == null || relationIds.isEmpty()) {
            return Set.of();
        }
        return contentRelationMapper.selectList(new QueryWrapper<ContentRelation>()
                        .eq("relation_type", relationType)
                        .in("relation_id", relationIds)
                        .eq("status", ACTIVE)
                        .eq("is_deleted", NOT_DELETED))
                .stream()
                .map(ContentRelation::getContentId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private ContentBatch loadContentBatch(List<Content> contents, Long userId) {
        ContentBatch batch = new ContentBatch();
        if (contents == null || contents.isEmpty()) {
            return batch;
        }
        Set<Long> contentIds = contents.stream().map(Content::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> authorIds = contents.stream().map(Content::getAuthorId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ContentRelation> relations = safeList(contentRelationMapper.selectList(new QueryWrapper<ContentRelation>()
                .in("content_id", contentIds)
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .orderByAsc("content_id")
                .orderByAsc("id")));
        batch.relations = relations.stream().collect(Collectors.groupingBy(
                ContentRelation::getContentId, LinkedHashMap::new, Collectors.toList()));

        Set<Long> teamIds = relationIds(relations, TEAM);
        Set<Long> playerIds = relationIds(relations, PLAYER);
        Set<Long> matchIds = relationIds(relations, MATCH);
        Set<Long> leagueIds = relationIds(relations, "LEAGUE");
        batch.players.putAll(indexById(safeBatchPlayers(playerIds), FootballPlayer::getId));
        batch.matches.putAll(indexById(safeBatchMatches(matchIds), MatchInfo::getId));
        batch.leagues.putAll(indexById(safeBatchLeagues(leagueIds), FootballLeague::getId));
        batch.matches.values().forEach(match -> {
            teamIds.add(match.getHomeTeamId());
            teamIds.add(match.getAwayTeamId());
        });
        batch.teams.putAll(indexById(safeBatchTeams(teamIds), FootballTeam::getId));

        List<Comment> comments = safeList(commentMapper.selectList(new QueryWrapper<Comment>()
                .eq("target_type", "CONTENT")
                .in("target_id", contentIds)
                .eq("parent_id", 0)
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)));
        LocalDateTime now = LocalDateTime.now();
        Comparator<Comment> hottest = Comparator
                .comparing((Comment c) -> CommentService.calculateHotScore(
                        nvl(c.getLikeCount()), nvl(c.getReplyCount()), c.getCreateTime(), now))
                .thenComparing(Comment::getCreateTime, Comparator.nullsFirst(Comparator.naturalOrder()));
        comments.forEach(comment -> batch.hotComments.merge(comment.getTargetId(), comment,
                (left, right) -> hottest.compare(left, right) >= 0 ? left : right));
        comments.stream().map(Comment::getUserId).filter(Objects::nonNull).forEach(authorIds::add);

        if (!authorIds.isEmpty()) {
            List<UserProfile> profiles = safeList(userProfileMapper.selectList(new QueryWrapper<UserProfile>()
                    .in("user_id", authorIds)
                    .eq("status", ACTIVE)
                    .eq("is_deleted", NOT_DELETED)));
            batch.profiles = profiles.stream().collect(Collectors.toMap(
                    UserProfile::getUserId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        }
        if (userId != null) {
            batch.likedContentIds = safeList(likeRecordMapper.selectList(new QueryWrapper<LikeRecord>()
                            .eq("user_id", userId)
                            .eq("target_type", "CONTENT")
                            .in("target_id", contentIds)
                            .eq("status", ACTIVE)))
                    .stream().map(LikeRecord::getTargetId).collect(Collectors.toSet());
            batch.favoritedContentIds = safeList(favoriteRecordMapper.selectList(new QueryWrapper<FavoriteRecord>()
                            .eq("user_id", userId)
                            .eq("target_type", "CONTENT")
                            .in("target_id", contentIds)
                            .eq("status", ACTIVE)))
                    .stream().map(FavoriteRecord::getTargetId).collect(Collectors.toSet());
        }
        return batch;
    }

    private MatchBatch loadMatchBatch(List<MatchInfo> matches) {
        MatchBatch batch = new MatchBatch();
        if (matches == null || matches.isEmpty()) {
            return batch;
        }
        Set<Long> matchIds = matches.stream().map(MatchInfo::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> leagueIds = matches.stream().map(MatchInfo::getLeagueId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> teamIds = new LinkedHashSet<>();
        matches.forEach(match -> {
            teamIds.add(match.getHomeTeamId());
            teamIds.add(match.getAwayTeamId());
        });
        batch.leagues = indexById(safeBatchLeagues(leagueIds), FootballLeague::getId);
        batch.teams = indexById(safeBatchTeams(teamIds), FootballTeam::getId);

        List<MatchReport> reports = safeList(matchReportMapper.selectList(new QueryWrapper<MatchReport>()
                .in("match_id", matchIds)
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .orderByAsc("id")));
        reports.forEach(report -> batch.reports.putIfAbsent(report.getMatchId(), report));

        List<MatchEvent> events = safeList(matchEventMapper.selectList(new QueryWrapper<MatchEvent>()
                .in("match_id", matchIds)
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .orderByDesc("minute")
                .orderByDesc("id")));
        events.forEach(event -> batch.events.putIfAbsent(event.getMatchId(), event));
        Set<Long> playerIds = batch.events.values().stream().map(MatchEvent::getPlayerId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        batch.players = indexById(safeBatchPlayers(playerIds), FootballPlayer::getId);
        return batch;
    }

    private RelationTagVO relationTag(ContentRelation relation, UserContext user, ContentBatch batch) {
        RelationTagVO vo = new RelationTagVO();
        vo.setRelationType(relation.getRelationType());
        vo.setRelationId(relation.getRelationId());
        vo.setRelationName(relationName(relation.getRelationType(), relation.getRelationId(), batch));
        vo.setFollowed(isRelationFollowed(relation, user));
        return vo;
    }

    private boolean isRelationFollowed(ContentRelation relation, UserContext user) {
        if (user.userId == null) {
            return false;
        }
        if (TEAM.equals(relation.getRelationType())) {
            return user.followedTeamIds.contains(relation.getRelationId());
        }
        if (PLAYER.equals(relation.getRelationType())) {
            return user.followedPlayerIds.contains(relation.getRelationId());
        }
        return false;
    }

    private String relationName(String type, Long id, ContentBatch batch) {
        if (TEAM.equals(type)) {
            FootballTeam team = batch.teams.get(id);
            return team == null ? null : team.getTeamName();
        }
        if (PLAYER.equals(type)) {
            FootballPlayer player = batch.players.get(id);
            return player == null ? null : player.getPlayerName();
        }
        if (MATCH.equals(type)) {
            MatchInfo match = batch.matches.get(id);
            if (match == null) {
                return null;
            }
            return teamName(batch.teams.get(match.getHomeTeamId())) + " vs "
                    + teamName(batch.teams.get(match.getAwayTeamId()));
        }
        if ("LEAGUE".equals(type)) {
            FootballLeague league = batch.leagues.get(id);
            return league == null ? null : league.getLeagueName();
        }
        return null;
    }

    private AuthorVO author(Long userId, Map<Long, UserProfile> profiles) {
        AuthorVO vo = new AuthorVO();
        vo.setUserId(userId);
        UserProfile profile = userId == null ? null : profiles.get(userId);
        vo.setNickname(profile == null ? null : profile.getNickname());
        vo.setAvatarUrl(profile == null ? null : profile.getAvatarUrl());
        vo.setVerified(false);
        return vo;
    }

    private HotCommentVO hotComment(Long contentId, ContentBatch batch) {
        Comment comment = batch.hotComments.get(contentId);
        if (comment == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        AuthorVO author = author(comment.getUserId(), batch.profiles);
        HotCommentVO vo = new HotCommentVO();
        vo.setCommentId(comment.getId());
        vo.setContentId(contentId);
        vo.setUserId(comment.getUserId());
        vo.setNickname(author.getNickname());
        vo.setAvatarUrl(author.getAvatarUrl());
        vo.setContent(comment.getContentText());
        vo.setLikeCount(nvl(comment.getLikeCount()));
        vo.setReplyCount(nvl(comment.getReplyCount()));
        vo.setHotScore(CommentService.calculateHotScore(nvl(comment.getLikeCount()), nvl(comment.getReplyCount()), comment.getCreateTime(), now));
        return vo;
    }

    private MatchTeamVO matchTeam(FootballTeam team, Integer score) {
        MatchTeamVO vo = new MatchTeamVO();
        if (team != null) {
            vo.setTeamId(team.getId());
            vo.setTeamName(team.getTeamName());
            vo.setLogoUrl(team.getLogoUrl());
        }
        vo.setScore(score);
        return vo;
    }

    private String teamName(FootballTeam team) {
        return team == null ? null : team.getTeamName();
    }

    private String leagueName(Long leagueId) {
        FootballLeague league = leagueMapper.selectById(leagueId);
        return league == null ? null : league.getLeagueName();
    }

    private String eventSummary(MatchEvent event, Map<Long, FootballPlayer> players) {
        if (event == null) {
            return null;
        }
        FootballPlayer player = event.getPlayerId() == null ? null : players.get(event.getPlayerId());
        String minute = event.getMinute() == null ? "" : event.getMinute() + "'";
        String name = player == null ? "" : " " + player.getPlayerName();
        return (minute + " " + event.getEventType() + name).trim();
    }

    private Set<Long> relationIds(List<ContentRelation> relations, String type) {
        return relations.stream().filter(relation -> type.equals(relation.getRelationType()))
                .map(ContentRelation::getRelationId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<FootballTeam> safeBatchTeams(Set<Long> ids) {
        return ids.isEmpty() ? List.of() : safeList(teamMapper.selectBatchIds(ids));
    }

    private List<FootballPlayer> safeBatchPlayers(Set<Long> ids) {
        return ids.isEmpty() ? List.of() : safeList(playerMapper.selectBatchIds(ids));
    }

    private List<MatchInfo> safeBatchMatches(Set<Long> ids) {
        return ids.isEmpty() ? List.of() : safeList(matchInfoMapper.selectBatchIds(ids));
    }

    private List<FootballLeague> safeBatchLeagues(Set<Long> ids) {
        return ids.isEmpty() ? List.of() : safeList(leagueMapper.selectBatchIds(ids));
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private <T> Map<Long, T> indexById(List<T> values, Function<T, Long> idExtractor) {
        return values.stream().collect(Collectors.toMap(idExtractor, Function.identity(),
                (left, right) -> left, LinkedHashMap::new));
    }

    private int countMatches(Long leagueId, String status) {
        Long count = matchInfoMapper.selectCount(new QueryWrapper<MatchInfo>()
                .eq("league_id", leagueId)
                .eq("match_status", status)
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED));
        return count == null ? 0 : count.intValue();
    }

    private List<FeedCardVO> deduplicate(List<FeedCardVO> cards) {
        Map<String, FeedCardVO> map = new LinkedHashMap<>();
        for (FeedCardVO card : cards) {
            map.merge(card.getCardId(), card, (oldCard, newCard) ->
                    oldCard.getScore() != null && newCard.getScore() != null && oldCard.getScore() >= newCard.getScore()
                            ? oldCard : newCard);
        }
        return new ArrayList<>(map.values());
    }

    private FeedPageResult page(List<FeedCardVO> cards, long pageNum, long pageSize) {
        long safePageNum = pageNum <= 0 ? 1 : pageNum;
        long safePageSize = pageSize <= 0 ? 10 : Math.min(pageSize, 100);
        int from = (int) Math.min((safePageNum - 1) * safePageSize, cards.size());
        int to = (int) Math.min(from + safePageSize, cards.size());
        return FeedPageResult.of(cards.subList(from, to), cards.size(), safePageNum, safePageSize);
    }

    private UserContext currentUserContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUserContext loginUser)) {
            return UserContext.anonymous();
        }
        UserContext context = new UserContext();
        context.userId = loginUser.getUserId();
        UserProfile profile = userProfileMapper.selectOne(new QueryWrapper<UserProfile>()
                .eq("user_id", loginUser.getUserId())
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .last("LIMIT 1"));
        context.mainTeamId = profile == null ? null : profile.getMainTeamId();
        List<FollowRecord> follows = followRecordMapper.selectList(new QueryWrapper<FollowRecord>()
                .eq("user_id", loginUser.getUserId())
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED));
        for (FollowRecord follow : follows) {
            if (TEAM.equals(follow.getFollowType())) {
                context.followedTeamIds.add(follow.getTargetId());
            } else if (PLAYER.equals(follow.getFollowType())) {
                context.followedPlayerIds.add(follow.getTargetId());
            } else if (USER.equals(follow.getFollowType())) {
                context.followedUserIds.add(follow.getTargetId());
            }
        }
        context.leagueIds.addAll(leaguesForTeams(context.followedTeamIds));
        if (context.mainTeamId != null) {
            context.leagueIds.addAll(leaguesForTeams(Set.of(context.mainTeamId)));
        }
        return context;
    }

    private Set<Long> leaguesForTeams(Set<Long> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) {
            return Set.of();
        }
        List<MatchInfo> matches = matchInfoMapper.selectList(new QueryWrapper<MatchInfo>()
                .and(w -> w.in("home_team_id", teamIds).or().in("away_team_id", teamIds))
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED));
        return matches.stream().map(MatchInfo::getLeagueId).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private double decimal(BigDecimal value) {
        return value == null ? 0D : value.doubleValue();
    }

    private static class ContentBatch {
        private Map<Long, List<ContentRelation>> relations = Collections.emptyMap();
        private Map<Long, UserProfile> profiles = Collections.emptyMap();
        private final Map<Long, FootballTeam> teams = new LinkedHashMap<>();
        private final Map<Long, FootballPlayer> players = new LinkedHashMap<>();
        private final Map<Long, MatchInfo> matches = new LinkedHashMap<>();
        private final Map<Long, FootballLeague> leagues = new LinkedHashMap<>();
        private final Map<Long, Comment> hotComments = new LinkedHashMap<>();
        private Set<Long> likedContentIds = Collections.emptySet();
        private Set<Long> favoritedContentIds = Collections.emptySet();
    }

    private static class MatchBatch {
        private Map<Long, FootballLeague> leagues = Collections.emptyMap();
        private Map<Long, FootballTeam> teams = Collections.emptyMap();
        private Map<Long, FootballPlayer> players = Collections.emptyMap();
        private final Map<Long, MatchReport> reports = new LinkedHashMap<>();
        private final Map<Long, MatchEvent> events = new LinkedHashMap<>();
    }

    private static class UserContext {
        private Long userId;
        private Long mainTeamId;
        private final Set<Long> followedTeamIds = new HashSet<>();
        private final Set<Long> followedPlayerIds = new HashSet<>();
        private final Set<Long> followedUserIds = new HashSet<>();
        private final Set<Long> leagueIds = new HashSet<>();

        private static UserContext anonymous() {
            return new UserContext();
        }
    }
}
