package com.southstand.card.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southstand.card.model.HomeFeedUserContext;
import com.southstand.card.vo.FeedCardVO;
import com.southstand.card.vo.HotCommentCardPayload;
import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.content.vo.AuthorVO;
import com.southstand.interaction.entity.Comment;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.interaction.service.CommentService;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.UserProfileMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class HotCommentCardService {
    private static final String ACTIVE = "ACTIVE";
    private static final int NOT_DELETED = 0;
    private final CommentMapper commentMapper;
    private final ContentMapper contentMapper;
    private final UserProfileMapper profileMapper;

    public HotCommentCardService(CommentMapper commentMapper, ContentMapper contentMapper,
                                 UserProfileMapper profileMapper) {
        this.commentMapper = commentMapper; this.contentMapper = contentMapper; this.profileMapper = profileMapper;
    }

    public List<FeedCardVO> candidates(HomeFeedUserContext ignored) {
        List<Comment> comments = safe(commentMapper.selectList(new QueryWrapper<Comment>()
                .eq("target_type", "CONTENT").eq("parent_id", 0).eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED).orderByDesc("create_time").orderByAsc("id").last("LIMIT 200")));
        if (comments.isEmpty()) return List.of();
        Set<Long> contentIds = comments.stream().map(Comment::getTargetId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Content> contents = safe(contentMapper.selectList(new QueryWrapper<Content>()
                .in("id", contentIds).eq("status", "PUBLISHED").eq("is_deleted", NOT_DELETED))).stream()
                .collect(Collectors.toMap(Content::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Set<Long> authorIds = comments.stream().map(Comment::getUserId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, UserProfile> profiles = authorIds.isEmpty() ? Map.of() : safe(profileMapper.selectList(
                new QueryWrapper<UserProfile>().in("user_id", authorIds).eq("status", ACTIVE)
                        .eq("is_deleted", NOT_DELETED))).stream().collect(Collectors.toMap(
                UserProfile::getUserId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        LocalDateTime now = LocalDateTime.now();
        return comments.stream().filter(c -> contents.containsKey(c.getTargetId()) && profiles.containsKey(c.getUserId()))
                .sorted(Comparator.comparing((Comment c) -> hot(c, now)).reversed()
                        .thenComparing(Comment::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Comment::getId))
                .limit(10).map(c -> card(c, contents.get(c.getTargetId()), profiles.get(c.getUserId()), now)).toList();
    }

    private FeedCardVO card(Comment comment, Content content, UserProfile profile, LocalDateTime now) {
        BigDecimal score = hot(comment, now);
        AuthorVO author = new AuthorVO(); author.setUserId(comment.getUserId()); author.setNickname(profile.getNickname());
        author.setAvatarUrl(profile.getAvatarUrl()); author.setVerified(false);
        String text = truncate(comment.getContentText(), 280);
        FeedCardVO card = new FeedCardVO(); card.setCardId("HOT_COMMENT_" + comment.getId());
        card.setCardKey("HOT_COMMENT:" + comment.getId()); card.setCardType("HOT_COMMENT");
        card.setAlgorithmVersion("AUX_RULE_V1"); card.setContentId(content.getId());
        card.setContentType(content.getContentType()); card.setTitle(content.getTitle()); card.setSummary(text);
        card.setAuthor(author); card.setLikeCount(nvl(comment.getLikeCount())); card.setCommentCount(nvl(comment.getReplyCount()));
        card.setReasonCode("HOT_COMMENT"); card.setReason("热门评论"); card.setScore(score.doubleValue());
        card.setPayload(new HotCommentCardPayload(comment.getId(), content.getId(), text, author,
                nvl(comment.getLikeCount()), nvl(comment.getReplyCount()), score,
                content.getTitle(), content.getContentType()));
        return card;
    }

    private BigDecimal hot(Comment c, LocalDateTime now) { return CommentService.calculateHotScore(nvl(c.getLikeCount()), nvl(c.getReplyCount()), c.getCreateTime(), now); }
    private String truncate(String value, int max) { if (value == null || value.length() <= max) return value; return value.substring(0, max - 1) + "…"; }
    private int nvl(Integer v) { return v == null ? 0 : v; }
    private <T> List<T> safe(List<T> rows) { return rows == null ? List.of() : rows; }
}
