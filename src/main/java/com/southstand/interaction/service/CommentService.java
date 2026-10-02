package com.southstand.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.common.result.PageResult;
import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.content.service.ContentService;
import com.southstand.interaction.dto.CreateCommentRequest;
import com.southstand.interaction.entity.Comment;
import com.southstand.interaction.entity.LikeRecord;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.interaction.mapper.LikeRecordMapper;
import com.southstand.football.matchdata.entity.FootballMatchPlayerStat;
import com.southstand.football.matchdata.mapper.FootballMatchPlayerStatMapper;
import com.southstand.interaction.vo.CommentVO;
import com.southstand.interaction.vo.CommentLikeToggleVO;
import com.southstand.interaction.vo.CreateCommentResponse;
import com.southstand.interaction.vo.HotCommentVO;
import com.southstand.notification.service.NotificationService;
import com.southstand.recommend.model.RecommendationBehaviorType;
import com.southstand.recommend.model.RecommendationTargetType;
import com.southstand.recommend.service.RecommendationBehaviorService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    public static final String TARGET_CONTENT = "CONTENT";
    public static final String TARGET_PLAYER_RATING = "PLAYER_RATING";
    public static final String TARGET_COMMENT = "COMMENT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_DELETED = "DELETED";

    private final CommentMapper commentMapper;
    private final ContentMapper contentMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final ContentService contentService;
    private final RecommendationBehaviorService recommendationBehaviorService;
    private final NotificationService notificationService;
    private final FootballMatchPlayerStatMapper matchPlayerStatMapper;

    @Autowired
    public CommentService(CommentMapper commentMapper,
            ContentMapper contentMapper,
            LikeRecordMapper likeRecordMapper,
            ContentService contentService,
            RecommendationBehaviorService recommendationBehaviorService,
            NotificationService notificationService,
            FootballMatchPlayerStatMapper matchPlayerStatMapper) {
        this.commentMapper = commentMapper;
        this.contentMapper = contentMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.contentService = contentService;
        this.recommendationBehaviorService = recommendationBehaviorService;
        this.notificationService = notificationService;
        this.matchPlayerStatMapper = matchPlayerStatMapper;
    }

    public CommentService(CommentMapper commentMapper, ContentMapper contentMapper, LikeRecordMapper likeRecordMapper,
            ContentService contentService, RecommendationBehaviorService recommendationBehaviorService) {
        this(commentMapper, contentMapper, likeRecordMapper, contentService, recommendationBehaviorService, null, null);
    }

    public CommentService(CommentMapper commentMapper, ContentMapper contentMapper, LikeRecordMapper likeRecordMapper,
            ContentService contentService, RecommendationBehaviorService recommendationBehaviorService,
            NotificationService notificationService) {
        this(commentMapper, contentMapper, likeRecordMapper, contentService, recommendationBehaviorService,
                notificationService, null);
    }

    public CommentService(CommentMapper commentMapper,
            ContentMapper contentMapper,
            LikeRecordMapper likeRecordMapper,
            ContentService contentService) {
        this(commentMapper, contentMapper, likeRecordMapper, contentService, null, null, null);
    }

    public PageResult<CommentVO> list(String targetType, Long targetId, String sort, Long parentId, long pageNum, long pageSize) {
        validateTargetType(targetType);
        validateTarget(targetType, targetId);
        long safePageNum = pageNum <= 0 ? 1 : pageNum;
        long safePageSize = Math.min(Math.max(pageSize <= 0 ? 10 : pageSize, 1), 100);
        long actualParentId = parentId == null ? 0L : parentId;

        List<Comment> comments = actualParentId == 0
                ? rootComments(targetType, targetId, sort)
                : replyComments(actualParentId, sort);

        int from = (int) Math.min((safePageNum - 1) * safePageSize, comments.size());
        int to = (int) Math.min(from + safePageSize, comments.size());
        Long userId = currentUserIdOrNull();
        List<CommentVO> records = new ArrayList<>();
        for (Comment comment : comments.subList(from, to)) {
            records.add(toVO(comment, userId, actualParentId == 0));
        }
        return PageResult.of(records, comments.size(), safePageNum, safePageSize);
    }

    public PageResult<CommentVO> replies(Long rootCommentId, String sort, long pageNum, long pageSize) {
        Comment root = requireActiveComment(rootCommentId);
        Long rootId = root.getParentId() == null || root.getParentId() == 0 ? root.getId() : root.getRootId();
        long safePageNum = pageNum <= 0 ? 1 : pageNum;
        long safePageSize = Math.min(Math.max(pageSize <= 0 ? 10 : pageSize, 1), 100);
        List<Comment> replies = replyComments(rootId, sort);
        int from = (int) Math.min((safePageNum - 1) * safePageSize, replies.size());
        int to = (int) Math.min(from + safePageSize, replies.size());
        Long userId = currentUserIdOrNull();
        List<CommentVO> records = replies.subList(from, to).stream()
                .map(comment -> toVO(comment, userId, false))
                .toList();
        return PageResult.of(records, replies.size(), safePageNum, safePageSize);
    }

    @Transactional(rollbackFor = Exception.class)
    public CreateCommentResponse create(CreateCommentRequest request) {
        String targetType = resolveTargetType(request);
        Long targetId = resolveTargetId(request);
        String contentText = resolveContentText(request);
        validateTargetType(targetType);
        validateTarget(targetType, targetId);
        Long userId = CurrentUserHolder.get().getUserId();
        Long parentId = request.getParentId() == null ? 0L : request.getParentId();

        Comment parent = null;
        Long rootId = null;
        Long replyToUserId = request.getReplyToUserId();
        if (parentId > 0) {
            parent = commentMapper.selectById(parentId);
            if (parent == null || Integer.valueOf(1).equals(parent.getIsDeleted())
                    || !STATUS_ACTIVE.equals(parent.getStatus())
                    || !targetType.equals(parent.getTargetType())
                    || !targetId.equals(parent.getTargetId())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "parent comment not found");
            }
            rootId = parent.getParentId() == null || parent.getParentId() == 0 ? parent.getId() : parent.getRootId();
            replyToUserId = replyToUserId == null ? parent.getUserId() : replyToUserId;
        }

        Comment comment = new Comment();
        LocalDateTime now = LocalDateTime.now();
        comment.setTargetType(targetType);
        comment.setTargetId(targetId);
        comment.setParentId(parentId);
        comment.setRootId(rootId);
        comment.setReplyToUserId(replyToUserId);
        comment.setUserId(userId);
        comment.setContentText(contentText);
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setHotScore(BigDecimal.ZERO);
        comment.setIsTop(0);
        comment.setStatus(STATUS_ACTIVE);
        comment.setIsDeleted(0);
        comment.setCreateTime(now);
        commentMapper.insert(comment);
        if (parent == null) {
            comment.setRootId(comment.getId());
            commentMapper.update(null, new UpdateWrapper<Comment>()
                    .eq("id", comment.getId())
                    .set("root_id", comment.getId()));
        }

        if (TARGET_CONTENT.equals(targetType)) {
            contentMapper.update(null, new UpdateWrapper<Content>()
                    .eq("id", targetId)
                    .setSql("comment_count = comment_count + 1"));
        }
        if (parent != null) {
            commentMapper.update(null, new UpdateWrapper<Comment>()
                    .eq("id", rootId)
                    .setSql("reply_count = reply_count + 1"));
        }

        CreateCommentResponse response = new CreateCommentResponse();
        response.setCommentId(comment.getId());
        response.setParentId(parentId);
        response.setCreateTime(comment.getCreateTime());
        if (TARGET_CONTENT.equals(targetType) && recommendationBehaviorService != null) {
            recommendationBehaviorService.recordInteractionSafely(userId, RecommendationBehaviorType.COMMENT,
                    RecommendationTargetType.CONTENT, targetId);
        }
        if (TARGET_CONTENT.equals(targetType) && notificationService != null) {
            if (parent == null) notificationService.notifyContentCommented(userId, targetId, comment.getId());
            else notificationService.notifyCommentReplied(userId, replyToUserId, comment.getId(), targetId);
        }
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public CommentLikeToggleVO toggleLike(Long commentId) {
        Long userId = CurrentUserHolder.get().getUserId();
        Comment comment = requireActiveComment(commentId);
        LikeRecord record = likeRecordMapper.selectOne(new QueryWrapper<LikeRecord>()
                .eq("user_id", userId)
                .eq("target_type", TARGET_COMMENT)
                .eq("target_id", commentId)
                .last("LIMIT 1"));
        boolean liked;
        if (record == null) {
            LikeRecord created = new LikeRecord();
            created.setUserId(userId);
            created.setTargetType(TARGET_COMMENT);
            created.setTargetId(commentId);
            created.setStatus(STATUS_ACTIVE);
            likeRecordMapper.insert(created);
            commentMapper.update(null, new UpdateWrapper<Comment>()
                    .eq("id", commentId)
                    .setSql("like_count = like_count + 1"));
            liked = true;
        } else if (STATUS_ACTIVE.equals(record.getStatus())) {
            likeRecordMapper.update(null, new UpdateWrapper<LikeRecord>()
                    .eq("id", record.getId())
                    .set("status", STATUS_CANCELLED));
            commentMapper.update(null, new UpdateWrapper<Comment>()
                    .eq("id", commentId)
                    .setSql("like_count = GREATEST(like_count - 1, 0)"));
            liked = false;
        } else {
            likeRecordMapper.update(null, new UpdateWrapper<LikeRecord>()
                    .eq("id", record.getId())
                    .set("status", STATUS_ACTIVE));
            commentMapper.update(null, new UpdateWrapper<Comment>()
                    .eq("id", commentId)
                    .setSql("like_count = like_count + 1"));
            liked = true;
        }
        Comment updated = commentMapper.selectById(commentId);
        CommentLikeToggleVO vo = new CommentLikeToggleVO();
        vo.setCommentId(commentId);
        vo.setLiked(liked);
        vo.setLikeCount(updated == null ? nvl(comment.getLikeCount()) + (liked ? 1 : -1) : nvl(updated.getLikeCount()));
        if (liked && notificationService != null) notificationService.notifyCommentLiked(userId, commentId);
        return vo;
    }

    public List<HotCommentVO> hotComments(Long contentId, int limit) {
        contentService.requireVisibleContent(contentId);
        int safeLimit = Math.max(1, Math.min(limit <= 0 ? 3 : limit, 10));
        List<Comment> comments = rootComments(TARGET_CONTENT, contentId, "hot");
        return comments.stream().limit(safeLimit).map(this::toHotComment).toList();
    }

    public HotCommentVO firstHotComment(Long contentId) {
        List<HotCommentVO> comments = hotComments(contentId, 1);
        return comments.isEmpty() ? null : comments.get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long commentId) {
        LoginUserContext currentUser = CurrentUserHolder.get();
        Comment comment = requireActiveComment(commentId);
        if (!comment.getUserId().equals(currentUser.getUserId()) && !"ADMIN".equals(currentUser.getRoleType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        commentMapper.update(null, new UpdateWrapper<Comment>()
                .eq("id", commentId)
                .set("status", STATUS_DELETED)
                .set("is_deleted", 1));
        if (TARGET_CONTENT.equals(comment.getTargetType())) {
            contentMapper.update(null, new UpdateWrapper<Content>()
                    .eq("id", comment.getTargetId())
                    .setSql("comment_count = GREATEST(comment_count - 1, 0)"));
        }
        if (comment.getParentId() != null && comment.getParentId() > 0) {
            Long rootId = comment.getRootId() == null ? comment.getParentId() : comment.getRootId();
            commentMapper.update(null, new UpdateWrapper<Comment>()
                    .eq("id", rootId)
                    .setSql("reply_count = GREATEST(reply_count - 1, 0)"));
        }
        return true;
    }

    public static BigDecimal calculateHotScore(int likeCount, int replyCount, LocalDateTime createTime, LocalDateTime now) {
        int base = likeCount + replyCount * 2;
        double coefficient = 0.3;
        if (createTime != null && now != null) {
            long hours = Math.max(Duration.between(createTime, now).toHours(), 0);
            if (hours <= 2) {
                coefficient = 1.5;
            } else if (hours <= 12) {
                coefficient = 1.0;
            } else if (hours <= 24) {
                coefficient = 0.7;
            }
        }
        return BigDecimal.valueOf(base * coefficient).setScale(2, RoundingMode.HALF_UP);
    }

    private CommentVO toVO(Comment comment, Long currentUserId, boolean includeReplies) {
        CommentVO vo = new CommentVO();
        vo.setCommentId(comment.getId());
        vo.setTargetType(comment.getTargetType());
        vo.setTargetId(comment.getTargetId());
        vo.setParentId(comment.getParentId());
        vo.setRootId(comment.getRootId() == null && (comment.getParentId() == null || comment.getParentId() == 0) ? comment.getId() : comment.getRootId());
        vo.setReplyToUserId(comment.getReplyToUserId());
        vo.setReplyToNickname(replyToNickname(comment.getReplyToUserId()));
        vo.setAuthor(contentService.authorOf(comment.getUserId()));
        vo.setContentText(comment.getContentText());
        vo.setLikeCount(nvl(comment.getLikeCount()));
        vo.setReplyCount(nvl(comment.getReplyCount()));
        vo.setHotScore(calculateHotScore(nvl(comment.getLikeCount()), nvl(comment.getReplyCount()), comment.getCreateTime(), LocalDateTime.now()));
        vo.setLiked(isActiveLike(currentUserId, "COMMENT", comment.getId()));
        vo.setCreateTime(comment.getCreateTime());
        if (includeReplies) {
            vo.setReplies(replyVOs(comment.getId(), currentUserId));
        } else {
            vo.setReplies(List.of());
        }
        return vo;
    }

    private List<CommentVO> replyVOs(Long parentId, Long currentUserId) {
        List<Comment> replies = commentMapper.selectList(new QueryWrapper<Comment>()
                .eq("root_id", parentId)
                .eq("status", STATUS_ACTIVE)
                .eq("is_deleted", 0)
                .orderByAsc("create_time")
                .last("LIMIT 3"));
        List<CommentVO> result = new ArrayList<>();
        for (Comment reply : replies) {
            result.add(toVO(reply, currentUserId, false));
        }
        return result;
    }

    private List<Comment> rootComments(String targetType, Long targetId, String sort) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<Comment>()
                .eq("target_type", targetType)
                .eq("target_id", targetId)
                .eq("parent_id", 0)
                .eq("status", STATUS_ACTIVE)
                .eq("is_deleted", 0);
        if (isLatest(sort)) {
            wrapper.orderByDesc("create_time");
        } else {
            wrapper.orderByDesc("create_time").last("LIMIT 200");
        }
        List<Comment> comments = commentMapper.selectList(wrapper);
        if (!isLatest(sort)) {
            sortHot(comments);
        }
        return comments;
    }

    private List<Comment> replyComments(Long rootId, String sort) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<Comment>()
                .eq("root_id", rootId)
                .ne("parent_id", 0)
                .eq("status", STATUS_ACTIVE)
                .eq("is_deleted", 0);
        if (isLatest(sort)) {
            wrapper.orderByDesc("create_time");
        } else {
            wrapper.orderByDesc("create_time").last("LIMIT 200");
        }
        List<Comment> comments = commentMapper.selectList(wrapper);
        if (!isLatest(sort)) {
            sortHot(comments);
        }
        return comments;
    }

    private void sortHot(List<Comment> comments) {
        LocalDateTime now = LocalDateTime.now();
        comments.sort(Comparator
                .comparing((Comment c) -> calculateHotScore(nvl(c.getLikeCount()), nvl(c.getReplyCount()), c.getCreateTime(), now))
                .reversed()
                .thenComparing(Comment::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
    }

    private HotCommentVO toHotComment(Comment comment) {
        HotCommentVO vo = new HotCommentVO();
        vo.setCommentId(comment.getId());
        vo.setContentId(comment.getTargetId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContentText());
        vo.setLikeCount(nvl(comment.getLikeCount()));
        vo.setReplyCount(nvl(comment.getReplyCount()));
        vo.setHotScore(calculateHotScore(nvl(comment.getLikeCount()), nvl(comment.getReplyCount()), comment.getCreateTime(), LocalDateTime.now()));
        var author = contentService.authorOf(comment.getUserId());
        vo.setNickname(author.getNickname());
        vo.setAvatarUrl(author.getAvatarUrl());
        return vo;
    }

    private boolean isActiveLike(Long userId, String targetType, Long targetId) {
        if (userId == null) {
            return false;
        }
        LikeRecord record = likeRecordMapper.selectOne(new QueryWrapper<LikeRecord>()
                .eq("user_id", userId)
                .eq("target_type", targetType)
                .eq("target_id", targetId)
                .last("LIMIT 1"));
        return record != null && STATUS_ACTIVE.equals(record.getStatus());
    }

    private Comment requireActiveComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || Integer.valueOf(1).equals(comment.getIsDeleted()) || !STATUS_ACTIVE.equals(comment.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "comment not found");
        }
        return comment;
    }

    private String resolveTargetType(CreateCommentRequest request) {
        return isBlank(request.getTargetType()) ? TARGET_CONTENT : request.getTargetType().trim();
    }

    private Long resolveTargetId(CreateCommentRequest request) {
        Long targetId = request.getTargetId() == null ? request.getContentId() : request.getTargetId();
        if (targetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "contentId is required");
        }
        return targetId;
    }

    private String resolveContentText(CreateCommentRequest request) {
        String text = request.getContentText() == null ? request.getContent() : request.getContentText();
        if (isBlank(text)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "comment content is required");
        }
        String trimmed = text.trim();
        if (trimmed.length() > 1000) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "comment content length must be 1-1000");
        }
        return trimmed;
    }

    private String replyToNickname(Long replyToUserId) {
        if (replyToUserId == null) {
            return null;
        }
        return contentService.authorOf(replyToUserId).getNickname();
    }

    private boolean isLatest(String sort) {
        return "latest".equalsIgnoreCase(sort) || "time".equalsIgnoreCase(sort);
    }

    private Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUserContext context) {
            return context.getUserId();
        }
        return null;
    }

    private void validateTargetType(String targetType) {
        if (!TARGET_CONTENT.equals(targetType) && !TARGET_PLAYER_RATING.equals(targetType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "unsupported targetType");
        }
    }

    private void validateTarget(String targetType, Long targetId) {
        if (TARGET_CONTENT.equals(targetType)) {
            contentService.requireVisibleContent(targetId);
            return;
        }
        if (matchPlayerStatMapper == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "player rating target not found");
        }
        FootballMatchPlayerStat stat = matchPlayerStatMapper.selectById(targetId);
        if (stat == null || Integer.valueOf(1).equals(stat.getIsDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "player rating target not found");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
