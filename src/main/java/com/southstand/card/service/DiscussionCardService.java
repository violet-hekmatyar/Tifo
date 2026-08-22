package com.southstand.card.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southstand.card.model.HomeFeedUserContext;
import com.southstand.card.vo.DiscussionCardPayload;
import com.southstand.card.vo.FeedCardVO;
import com.southstand.card.vo.RelationTagVO;
import com.southstand.content.entity.Content;
import com.southstand.content.entity.ContentRelation;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.content.mapper.ContentRelationMapper;
import com.southstand.content.vo.AuthorVO;
import com.southstand.interaction.entity.Comment;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.interaction.service.CommentService;
import com.southstand.interaction.vo.HotCommentVO;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.UserProfileMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DiscussionCardService {
    private static final String ACTIVE = "ACTIVE";
    private static final int NOT_DELETED = 0;
    private final ContentMapper contentMapper;
    private final ContentRelationMapper relationMapper;
    private final CommentMapper commentMapper;
    private final UserProfileMapper profileMapper;

    public DiscussionCardService(ContentMapper contentMapper, ContentRelationMapper relationMapper,
                                 CommentMapper commentMapper, UserProfileMapper profileMapper) {
        this.contentMapper = contentMapper; this.relationMapper = relationMapper;
        this.commentMapper = commentMapper; this.profileMapper = profileMapper;
    }

    public List<FeedCardVO> candidates(HomeFeedUserContext user) {
        List<Content> contents = safe(contentMapper.selectList(new QueryWrapper<Content>()
                .eq("content_type", "POST").eq("status", "PUBLISHED").eq("is_deleted", NOT_DELETED)
                .orderByDesc("comment_count").orderByDesc("like_count")
                .orderByDesc("publish_time").orderByAsc("id").last("LIMIT 20")));
        if (contents.isEmpty()) return List.of();
        Set<Long> ids = contents.stream().map(Content::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<ContentRelation> relations = safe(relationMapper.selectList(new QueryWrapper<ContentRelation>()
                .in("content_id", ids).eq("status", ACTIVE).eq("is_deleted", NOT_DELETED)
                .orderByAsc("content_id").orderByAsc("id")));
        Map<Long, List<ContentRelation>> byContent = relations.stream().collect(Collectors.groupingBy(
                ContentRelation::getContentId, LinkedHashMap::new, Collectors.toList()));
        List<Comment> comments = safe(commentMapper.selectList(new QueryWrapper<Comment>()
                .eq("target_type", "CONTENT").in("target_id", ids).eq("parent_id", 0)
                .eq("status", ACTIVE).eq("is_deleted", NOT_DELETED)));
        LocalDateTime now = LocalDateTime.now();
        Comparator<Comment> hot = Comparator.comparing((Comment c) -> CommentService.calculateHotScore(
                nvl(c.getLikeCount()), nvl(c.getReplyCount()), c.getCreateTime(), now))
                .thenComparing(Comment::getCreateTime, Comparator.nullsFirst(Comparator.naturalOrder()));
        Map<Long, Comment> hotComments = new LinkedHashMap<>();
        comments.forEach(c -> hotComments.merge(c.getTargetId(), c, (a, b) -> hot.compare(a, b) >= 0 ? a : b));
        Set<Long> authorIds = contents.stream().map(Content::getAuthorId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        comments.stream().map(Comment::getUserId).filter(Objects::nonNull).forEach(authorIds::add);
        Map<Long, UserProfile> profiles = authorIds.isEmpty() ? Map.of() : safe(profileMapper.selectList(
                new QueryWrapper<UserProfile>().in("user_id", authorIds).eq("status", ACTIVE)
                        .eq("is_deleted", NOT_DELETED))).stream().collect(Collectors.toMap(
                UserProfile::getUserId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        List<Scored> scored = new ArrayList<>();
        for (Content content : contents) {
            List<ContentRelation> tags = byContent.getOrDefault(content.getId(), List.of());
            double score = score(content, tags, user, now);
            String reasonCode = reason(tags, user);
            scored.add(new Scored(toCard(content, tags, profiles, hotComments.get(content.getId()), reasonCode, score, now), score));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed()
                .thenComparing(s -> s.card().getPublishTime(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(s -> s.card().getContentId()));
        return scored.stream().limit(10).map(Scored::card).toList();
    }

    private FeedCardVO toCard(Content content, List<ContentRelation> relations,
                              Map<Long, UserProfile> profiles, Comment hotComment,
                              String reasonCode, double score, LocalDateTime now) {
        FeedCardVO card = new FeedCardVO();
        card.setCardId("DISCUSSION_" + content.getId()); card.setCardKey("DISCUSSION:" + content.getId());
        card.setCardType("DISCUSSION"); card.setAlgorithmVersion("AUX_RULE_V1");
        card.setContentId(content.getId()); card.setContentType(content.getContentType());
        card.setTitle(content.getTitle()); card.setSummary(content.getSummary()); card.setCoverUrl(content.getCoverUrl());
        AuthorVO author = author(content.getAuthorId(), profiles); card.setAuthor(author);
        card.setPublishTime(content.getPublishTime()); card.setCommentCount(nvl(content.getCommentCount()));
        card.setLikeCount(nvl(content.getLikeCount())); card.setFavoriteCount(nvl(content.getFavoriteCount()));
        HotCommentVO hot = hot(hotComment, profiles, now); card.setHotComment(hot);
        List<RelationTagVO> tags = relations.stream().map(r -> relation(r)).toList(); card.setRelationTags(tags);
        card.setReasonCode(reasonCode); card.setReason(displayReason(reasonCode)); card.setScore(score);
        card.setPayload(new DiscussionCardPayload(content.getId(), content.getTitle(), content.getSummary(), author,
                content.getPublishTime(), nvl(content.getCommentCount()), nvl(content.getLikeCount()),
                nvl(content.getFavoriteCount()), hot, tags));
        return card;
    }

    private double score(Content c, List<ContentRelation> relations, HomeFeedUserContext user, LocalDateTime now) {
        double age = c.getPublishTime() == null ? 720D : Math.max(0D, Duration.between(c.getPublishTime(), now).toMinutes() / 60D);
        double value = nvl(c.getCommentCount()) * 5D + nvl(c.getLikeCount()) * 2D
                + nvl(c.getFavoriteCount()) * 3D + 30D * Math.exp(-age / 48D);
        for (ContentRelation relation : relations) {
            if ("TEAM".equals(relation.getRelationType()) && Objects.equals(relation.getRelationId(), user.mainTeamId())) value += 50D;
            else if ("TEAM".equals(relation.getRelationType()) && user.followedTeamIds().contains(relation.getRelationId())) value += 35D;
            else if ("PLAYER".equals(relation.getRelationType()) && user.followedPlayerIds().contains(relation.getRelationId())) value += 25D;
        }
        return value;
    }

    private String reason(List<ContentRelation> relations, HomeFeedUserContext user) {
        if (relations.stream().anyMatch(r -> "TEAM".equals(r.getRelationType()) && Objects.equals(r.getRelationId(), user.mainTeamId()))) return "MAIN_TEAM";
        if (relations.stream().anyMatch(r -> "TEAM".equals(r.getRelationType()) && user.followedTeamIds().contains(r.getRelationId()))) return "FOLLOWED_TEAM";
        if (relations.stream().anyMatch(r -> "PLAYER".equals(r.getRelationType()) && user.followedPlayerIds().contains(r.getRelationId()))) return "FOLLOWED_PLAYER";
        return "HOT_DISCUSSION";
    }

    private String displayReason(String code) { return switch (code) {
        case "MAIN_TEAM" -> "与你的主队有关"; case "FOLLOWED_TEAM" -> "你关注的球队";
        case "FOLLOWED_PLAYER" -> "你关注的球员"; default -> "热门讨论"; }; }
    private RelationTagVO relation(ContentRelation r) { RelationTagVO vo = new RelationTagVO(); vo.setRelationType(r.getRelationType()); vo.setRelationId(r.getRelationId()); return vo; }
    private AuthorVO author(Long id, Map<Long, UserProfile> profiles) { AuthorVO vo = new AuthorVO(); vo.setUserId(id); UserProfile p = profiles.get(id); if (p != null) { vo.setNickname(p.getNickname()); vo.setAvatarUrl(p.getAvatarUrl()); } vo.setVerified(false); return vo; }
    private HotCommentVO hot(Comment c, Map<Long, UserProfile> profiles, LocalDateTime now) { if (c == null) return null; HotCommentVO vo = new HotCommentVO(); UserProfile p = profiles.get(c.getUserId()); vo.setCommentId(c.getId()); vo.setContentId(c.getTargetId()); vo.setUserId(c.getUserId()); vo.setNickname(p == null ? null : p.getNickname()); vo.setAvatarUrl(p == null ? null : p.getAvatarUrl()); vo.setContent(c.getContentText()); vo.setLikeCount(nvl(c.getLikeCount())); vo.setReplyCount(nvl(c.getReplyCount())); vo.setHotScore(CommentService.calculateHotScore(nvl(c.getLikeCount()), nvl(c.getReplyCount()), c.getCreateTime(), now)); return vo; }
    private int nvl(Integer v) { return v == null ? 0 : v; }
    private <T> List<T> safe(List<T> rows) { return rows == null ? List.of() : rows; }
    private record Scored(FeedCardVO card, double score) { }
}
