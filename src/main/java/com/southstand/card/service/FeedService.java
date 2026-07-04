package com.southstand.card.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.card.vo.FeedCardVO;
import com.southstand.card.vo.FeedPageResult;
import com.southstand.card.vo.HotLeagueVO;
import com.southstand.card.vo.RelationTagVO;
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
import com.southstand.interaction.mapper.FavoriteRecordMapper;
import com.southstand.interaction.mapper.LikeRecordMapper;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.UserProfileMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FeedService {

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
            FavoriteRecordMapper favoriteRecordMapper
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
        cards = deduplicate(cards);
        cards.sort(Comparator.comparing(FeedCardVO::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(card -> Objects.toString(card.getCardId(), "")));
        return page(cards, pageNum, pageSize);
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
        return contentMapper.selectList(wrapper).stream()
                .map(content -> toContentCard(content, user, personalizedOnly || user.userId != null))
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
        return matchInfoMapper.selectList(wrapper).stream()
                .map(match -> toMatchCard(match, user))
                .collect(Collectors.toList());
    }

    private FeedCardVO toContentCard(Content content, UserContext user, boolean personalized) {
        FeedCardVO card = new FeedCardVO();
        card.setCardId("CONTENT_" + content.getId());
        card.setCardType(CARD_CONTENT);
        card.setContentId(content.getId());
        card.setContentType(content.getContentType());
        card.setTitle(content.getTitle());
        card.setSummary(content.getSummary());
        card.setCoverUrl(content.getCoverUrl());
        card.setAuthor(author(content.getAuthorId()));
        List<ContentRelation> relations = contentRelations(content.getId());
        card.setRelationTags(relations.stream().map(relation -> relationTag(relation, user)).collect(Collectors.toList()));
        card.setLikeCount(nvl(content.getLikeCount()));
        card.setCommentCount(nvl(content.getCommentCount()));
        card.setFavoriteCount(nvl(content.getFavoriteCount()));
        card.setLiked(isLiked(user.userId, content.getId()));
        card.setFavorited(isFavorited(user.userId, content.getId()));
        card.setPublishTime(content.getPublishTime());
        card.setScore(contentScore(content, relations, user, personalized));
        return card;
    }

    private FeedCardVO toMatchCard(MatchInfo match, UserContext user) {
        MatchReport report = firstReport(match.getId());
        FeedCardVO card = new FeedCardVO();
        card.setCardId("MATCH_" + match.getId());
        card.setCardType(CARD_MATCH);
        card.setMatchId(match.getId());
        card.setLeagueId(match.getLeagueId());
        card.setLeagueName(leagueName(match.getLeagueId()));
        card.setHomeTeam(matchTeam(match.getHomeTeamId(), match.getHomeScore()));
        card.setAwayTeam(matchTeam(match.getAwayTeamId(), match.getAwayScore()));
        card.setHomeScore(match.getHomeScore());
        card.setAwayScore(match.getAwayScore());
        card.setMatchStatus(match.getMatchStatus());
        card.setMatchTime(match.getMatchTime());
        card.setEventSummary(eventSummary(match.getId()));
        card.setHasReport(report != null || Objects.equals(match.getHasReport(), 1));
        card.setReportContentId(report == null ? null : report.getContentId());
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

    private List<ContentRelation> contentRelations(Long contentId) {
        return contentRelationMapper.selectList(new QueryWrapper<ContentRelation>()
                .eq("content_id", contentId)
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .orderByAsc("id"));
    }

    private RelationTagVO relationTag(ContentRelation relation, UserContext user) {
        RelationTagVO vo = new RelationTagVO();
        vo.setRelationType(relation.getRelationType());
        vo.setRelationId(relation.getRelationId());
        vo.setRelationName(relationName(relation.getRelationType(), relation.getRelationId()));
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

    private String relationName(String type, Long id) {
        if (TEAM.equals(type)) {
            FootballTeam team = teamMapper.selectById(id);
            return team == null ? null : team.getTeamName();
        }
        if (PLAYER.equals(type)) {
            FootballPlayer player = playerMapper.selectById(id);
            return player == null ? null : player.getPlayerName();
        }
        if (MATCH.equals(type)) {
            MatchInfo match = matchInfoMapper.selectById(id);
            if (match == null) {
                return null;
            }
            return teamName(match.getHomeTeamId()) + " vs " + teamName(match.getAwayTeamId());
        }
        if ("LEAGUE".equals(type)) {
            return leagueName(id);
        }
        return null;
    }

    private AuthorVO author(Long userId) {
        AuthorVO vo = new AuthorVO();
        vo.setUserId(userId);
        UserProfile profile = userId == null ? null : userProfileMapper.selectOne(new QueryWrapper<UserProfile>()
                .eq("user_id", userId)
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .last("LIMIT 1"));
        vo.setNickname(profile == null ? null : profile.getNickname());
        vo.setAvatarUrl(profile == null ? null : profile.getAvatarUrl());
        vo.setVerified(false);
        return vo;
    }

    private boolean isLiked(Long userId, Long contentId) {
        if (userId == null) {
            return false;
        }
        Long count = likeRecordMapper.selectCount(new QueryWrapper<LikeRecord>()
                .eq("user_id", userId)
                .eq("target_type", "CONTENT")
                .eq("target_id", contentId)
                .eq("status", ACTIVE));
        return count != null && count > 0;
    }

    private boolean isFavorited(Long userId, Long contentId) {
        if (userId == null) {
            return false;
        }
        Long count = favoriteRecordMapper.selectCount(new QueryWrapper<FavoriteRecord>()
                .eq("user_id", userId)
                .eq("target_type", "CONTENT")
                .eq("target_id", contentId)
                .eq("status", ACTIVE));
        return count != null && count > 0;
    }

    private MatchTeamVO matchTeam(Long teamId, Integer score) {
        FootballTeam team = teamMapper.selectById(teamId);
        MatchTeamVO vo = new MatchTeamVO();
        if (team != null) {
            vo.setTeamId(team.getId());
            vo.setTeamName(team.getTeamName());
            vo.setLogoUrl(team.getLogoUrl());
        }
        vo.setScore(score);
        return vo;
    }

    private String teamName(Long teamId) {
        FootballTeam team = teamMapper.selectById(teamId);
        return team == null ? null : team.getTeamName();
    }

    private String leagueName(Long leagueId) {
        FootballLeague league = leagueMapper.selectById(leagueId);
        return league == null ? null : league.getLeagueName();
    }

    private String eventSummary(Long matchId) {
        MatchEvent event = matchEventMapper.selectOne(new QueryWrapper<MatchEvent>()
                .eq("match_id", matchId)
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .orderByDesc("minute")
                .orderByDesc("id")
                .last("LIMIT 1"));
        if (event == null) {
            return null;
        }
        FootballPlayer player = event.getPlayerId() == null ? null : playerMapper.selectById(event.getPlayerId());
        String minute = event.getMinute() == null ? "" : event.getMinute() + "'";
        String name = player == null ? "" : " " + player.getPlayerName();
        return (minute + " " + event.getEventType() + name).trim();
    }

    private MatchReport firstReport(Long matchId) {
        return matchReportMapper.selectOne(new QueryWrapper<MatchReport>()
                .eq("match_id", matchId)
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .orderByAsc("id")
                .last("LIMIT 1"));
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
