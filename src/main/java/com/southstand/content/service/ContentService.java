package com.southstand.content.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.content.dto.ContentRelationRequest;
import com.southstand.content.dto.CreatePostRequest;
import com.southstand.content.entity.Content;
import com.southstand.content.entity.ContentMedia;
import com.southstand.content.entity.ContentRelation;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.content.mapper.ContentMediaMapper;
import com.southstand.content.mapper.ContentRelationMapper;
import com.southstand.content.vo.AuthorVO;
import com.southstand.content.vo.ContentDetailVO;
import com.southstand.content.vo.CreatePostResponse;
import com.southstand.content.vo.MediaVO;
import com.southstand.content.vo.RelationVO;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.interaction.entity.FavoriteRecord;
import com.southstand.interaction.entity.LikeRecord;
import com.southstand.interaction.mapper.FavoriteRecordMapper;
import com.southstand.interaction.mapper.LikeRecordMapper;
import com.southstand.user.entity.SysUser;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserProfileMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentService {

    public static final String TARGET_CONTENT = "CONTENT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final ContentMapper contentMapper;
    private final ContentMediaMapper contentMediaMapper;
    private final ContentRelationMapper contentRelationMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final FavoriteRecordMapper favoriteRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final FootballTeamMapper footballTeamMapper;
    private final FootballPlayerMapper footballPlayerMapper;

    public ContentService(ContentMapper contentMapper,
            ContentMediaMapper contentMediaMapper,
            ContentRelationMapper contentRelationMapper,
            LikeRecordMapper likeRecordMapper,
            FavoriteRecordMapper favoriteRecordMapper,
            SysUserMapper sysUserMapper,
            UserProfileMapper userProfileMapper,
            FootballTeamMapper footballTeamMapper,
            FootballPlayerMapper footballPlayerMapper) {
        this.contentMapper = contentMapper;
        this.contentMediaMapper = contentMediaMapper;
        this.contentRelationMapper = contentRelationMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.favoriteRecordMapper = favoriteRecordMapper;
        this.sysUserMapper = sysUserMapper;
        this.userProfileMapper = userProfileMapper;
        this.footballTeamMapper = footballTeamMapper;
        this.footballPlayerMapper = footballPlayerMapper;
    }

    public ContentDetailVO detail(Long contentId) {
        Content content = requireVisibleContent(contentId);
        try {
            contentMapper.update(null, new UpdateWrapper<Content>()
                    .eq("id", contentId)
                    .setSql("view_count = view_count + 1"));
            content.setViewCount(nvl(content.getViewCount()) + 1);
        } catch (RuntimeException ignored) {
            // View count is best-effort and must not break content reading.
        }
        Long userId = currentUserIdOrNull();
        return toDetail(content, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public CreatePostResponse createPost(CreatePostRequest request) {
        Long userId = CurrentUserHolder.get().getUserId();
        if (isBlank(request.getBody()) && (request.getMediaUrls() == null || request.getMediaUrls().isEmpty())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "body or mediaUrls required");
        }
        validateRelations(request.getRelationList());

        LocalDateTime now = LocalDateTime.now();
        Content content = new Content();
        content.setContentType("POST");
        content.setContentFormat("POST_FORMAT");
        content.setCardType("CONTENT_CARD");
        content.setTitle(request.getTitle().trim());
        content.setSummary(summaryOf(request.getBody()));
        content.setBody(request.getBody());
        content.setCoverUrl(firstMediaUrl(request.getMediaUrls()));
        content.setAuthorId(userId);
        content.setSourceType("USER");
        content.setIsOfficial(0);
        content.setViewCount(0);
        content.setLikeCount(0);
        content.setCommentCount(0);
        content.setFavoriteCount(0);
        content.setHotScore(BigDecimal.ZERO);
        content.setPublishTime(now);
        content.setStatus(STATUS_PUBLISHED);
        content.setIsDeleted(0);
        contentMapper.insert(content);

        insertMedia(content.getId(), request.getMediaUrls());
        insertRelations(content.getId(), request.getRelationList());
        userProfileMapper.update(null, new UpdateWrapper<UserProfile>()
                .eq("user_id", userId)
                .setSql("post_count = post_count + 1"));

        CreatePostResponse response = new CreatePostResponse();
        response.setContentId(content.getId());
        response.setContentType(content.getContentType());
        response.setTitle(content.getTitle());
        response.setPublishTime(content.getPublishTime());
        return response;
    }

    public Content requireVisibleContent(Long contentId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null || Integer.valueOf(1).equals(content.getIsDeleted())
                || !(STATUS_PUBLISHED.equals(content.getStatus()) || STATUS_ACTIVE.equals(content.getStatus()))) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "content not found");
        }
        return content;
    }

    public AuthorVO authorOf(Long userId) {
        AuthorVO author = new AuthorVO();
        author.setUserId(userId);
        author.setNickname("user-" + userId);
        author.setVerified(false);

        UserProfile profile = userProfileMapper.selectOne(new QueryWrapper<UserProfile>()
                .eq("user_id", userId)
                .eq("is_deleted", 0)
                .last("LIMIT 1"));
        if (profile != null) {
            author.setNickname(profile.getNickname());
            author.setAvatarUrl(profile.getAvatarUrl());
        }
        SysUser user = sysUserMapper.selectById(userId);
        author.setVerified(user != null && "ADMIN".equals(user.getRoleType()));
        return author;
    }

    private ContentDetailVO toDetail(Content content, Long currentUserId) {
        ContentDetailVO vo = new ContentDetailVO();
        vo.setContentId(content.getId());
        vo.setContentType(content.getContentType());
        vo.setContentFormat(content.getContentFormat());
        vo.setCardType(content.getCardType());
        vo.setTitle(content.getTitle());
        vo.setSummary(content.getSummary());
        vo.setBody(content.getBody());
        vo.setCoverUrl(content.getCoverUrl());
        vo.setAuthor(authorOf(content.getAuthorId()));
        vo.setMediaList(mediaList(content.getId()));
        vo.setRelationList(relationList(content.getId()));
        vo.setLikeCount(nvl(content.getLikeCount()));
        vo.setCommentCount(nvl(content.getCommentCount()));
        vo.setFavoriteCount(nvl(content.getFavoriteCount()));
        vo.setViewCount(nvl(content.getViewCount()));
        vo.setLiked(isActiveLike(currentUserId, TARGET_CONTENT, content.getId()));
        vo.setFavorited(isActiveFavorite(currentUserId, TARGET_CONTENT, content.getId()));
        vo.setPublishTime(content.getPublishTime());
        return vo;
    }

    private List<MediaVO> mediaList(Long contentId) {
        List<ContentMedia> mediaList = contentMediaMapper.selectList(new QueryWrapper<ContentMedia>()
                .eq("content_id", contentId)
                .eq("status", STATUS_ACTIVE)
                .eq("is_deleted", 0)
                .orderByAsc("sort_order", "id"));
        List<MediaVO> result = new ArrayList<>();
        for (ContentMedia media : mediaList) {
            MediaVO vo = new MediaVO();
            vo.setMediaId(media.getId());
            vo.setMediaType(media.getMediaType());
            vo.setMediaUrl(media.getMediaUrl());
            vo.setThumbnailUrl(media.getThumbnailUrl());
            vo.setWidth(media.getWidth());
            vo.setHeight(media.getHeight());
            result.add(vo);
        }
        return result;
    }

    private List<RelationVO> relationList(Long contentId) {
        List<ContentRelation> relations = contentRelationMapper.selectList(new QueryWrapper<ContentRelation>()
                .eq("content_id", contentId)
                .eq("status", STATUS_ACTIVE)
                .eq("is_deleted", 0)
                .orderByAsc("id"));
        List<RelationVO> result = new ArrayList<>();
        for (ContentRelation relation : relations) {
            RelationVO vo = new RelationVO();
            vo.setRelationType(relation.getRelationType());
            vo.setRelationId(relation.getRelationId());
            vo.setRelationName(resolveRelationName(relation.getRelationType(), relation.getRelationId()));
            result.add(vo);
        }
        return result;
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

    private boolean isActiveFavorite(Long userId, String targetType, Long targetId) {
        if (userId == null) {
            return false;
        }
        FavoriteRecord record = favoriteRecordMapper.selectOne(new QueryWrapper<FavoriteRecord>()
                .eq("user_id", userId)
                .eq("target_type", targetType)
                .eq("target_id", targetId)
                .last("LIMIT 1"));
        return record != null && STATUS_ACTIVE.equals(record.getStatus());
    }

    private void validateRelations(List<ContentRelationRequest> relations) {
        if (relations == null) {
            return;
        }
        for (ContentRelationRequest relation : relations) {
            if ("TEAM".equals(relation.getRelationType())) {
                FootballTeam team = footballTeamMapper.selectById(relation.getRelationId());
                if (team == null || !"ACTIVE".equals(team.getStatus()) || Integer.valueOf(1).equals(team.getIsDeleted())) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "team not found");
                }
            } else if ("PLAYER".equals(relation.getRelationType())) {
                FootballPlayer player = footballPlayerMapper.selectById(relation.getRelationId());
                if (player == null || !"ACTIVE".equals(player.getStatus()) || Integer.valueOf(1).equals(player.getIsDeleted())) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "player not found");
                }
            } else if (!"MATCH".equals(relation.getRelationType())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "unsupported relation type");
            }
        }
    }

    private void insertMedia(Long contentId, List<String> mediaUrls) {
        if (mediaUrls == null) {
            return;
        }
        for (int i = 0; i < mediaUrls.size(); i++) {
            String url = mediaUrls.get(i);
            if (isBlank(url)) {
                continue;
            }
            ContentMedia media = new ContentMedia();
            media.setContentId(contentId);
            media.setMediaType("IMAGE");
            media.setMediaUrl(url);
            media.setSortOrder(i + 1);
            media.setStatus(STATUS_ACTIVE);
            media.setIsDeleted(0);
            contentMediaMapper.insert(media);
        }
    }

    private void insertRelations(Long contentId, List<ContentRelationRequest> relations) {
        if (relations == null) {
            return;
        }
        for (ContentRelationRequest request : relations) {
            ContentRelation relation = new ContentRelation();
            relation.setContentId(contentId);
            relation.setRelationType(request.getRelationType());
            relation.setRelationId(request.getRelationId());
            relation.setConfidence(BigDecimal.ONE);
            relation.setSourceType("USER");
            relation.setStatus(STATUS_ACTIVE);
            relation.setIsDeleted(0);
            contentRelationMapper.insert(relation);
        }
    }

    private String resolveRelationName(String relationType, Long relationId) {
        try {
            if ("TEAM".equals(relationType)) {
                FootballTeam team = footballTeamMapper.selectById(relationId);
                return team == null ? null : team.getTeamName();
            }
            if ("PLAYER".equals(relationType)) {
                FootballPlayer player = footballPlayerMapper.selectById(relationId);
                return player == null ? null : player.getPlayerName();
            }
            if ("MATCH".equals(relationType)) {
                return "match-" + relationId;
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUserContext context) {
            return context.getUserId();
        }
        return null;
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private static String firstMediaUrl(List<String> mediaUrls) {
        if (mediaUrls == null) {
            return null;
        }
        for (String url : mediaUrls) {
            if (!isBlank(url)) {
                return url;
            }
        }
        return null;
    }

    private static String summaryOf(String body) {
        if (isBlank(body)) {
            return null;
        }
        String text = body.trim();
        return text.length() <= 120 ? text : text.substring(0, 120);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
