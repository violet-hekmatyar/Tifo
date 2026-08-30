package com.southstand.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.common.result.PageResult;
import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.interaction.entity.Comment;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.notification.entity.Notification;
import com.southstand.notification.mapper.NotificationMapper;
import com.southstand.notification.model.NotificationTargetType;
import com.southstand.notification.model.NotificationType;
import com.southstand.notification.vo.NotificationVO;
import com.southstand.notification.vo.ReadAllVO;
import com.southstand.notification.vo.UnreadCountVO;
import com.southstand.user.entity.SysUser;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserProfileMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class NotificationService {
    private static final Logger log=LoggerFactory.getLogger(NotificationService.class);
    private static final String ACTIVE="ACTIVE";
    private final NotificationMapper notificationMapper;
    private final NotificationWriter writer;
    private final ContentMapper contentMapper;
    private final CommentMapper commentMapper;
    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;

    public NotificationService(NotificationMapper notificationMapper, NotificationWriter writer,
            ContentMapper contentMapper, CommentMapper commentMapper, SysUserMapper sysUserMapper,
            UserProfileMapper userProfileMapper){
        this.notificationMapper=notificationMapper;this.writer=writer;this.contentMapper=contentMapper;
        this.commentMapper=commentMapper;this.sysUserMapper=sysUserMapper;this.userProfileMapper=userProfileMapper;
    }

    public void notifyContentLiked(Long actorId,Long contentId){
        safely(()->{Content c=contentMapper.selectById(contentId);if(c!=null) enqueue(build(c.getAuthorId(),actorId,
                NotificationType.CONTENT_LIKED,NotificationTargetType.CONTENT,contentId,null,null,
                "内容获赞",actorName(actorId)+" 点赞了你的内容","CONTENT_LIKED:"+actorId+":"+contentId));});
    }
    public void notifyContentCommented(Long actorId,Long contentId,Long commentId){
        safely(()->{Content c=contentMapper.selectById(contentId);if(c!=null) enqueue(build(c.getAuthorId(),actorId,
                NotificationType.CONTENT_COMMENTED,NotificationTargetType.CONTENT,contentId,NotificationTargetType.COMMENT,commentId,
                "新评论",actorName(actorId)+" 评论了你的内容","CONTENT_COMMENTED:"+commentId));});
    }
    public void notifyCommentReplied(Long actorId,Long recipientId,Long commentId,Long contentId){
        safely(()->enqueue(build(recipientId,actorId,NotificationType.COMMENT_REPLIED,NotificationTargetType.COMMENT,commentId,
                NotificationTargetType.CONTENT,contentId,"新回复",actorName(actorId)+" 回复了你的评论","COMMENT_REPLIED:"+commentId)));
    }
    public void notifyCommentLiked(Long actorId,Long commentId){
        safely(()->{Comment c=commentMapper.selectById(commentId);if(c!=null) enqueue(build(c.getUserId(),actorId,
                NotificationType.COMMENT_LIKED,NotificationTargetType.COMMENT,commentId,NotificationTargetType.CONTENT,c.getTargetId(),
                "评论获赞",actorName(actorId)+" 点赞了你的评论","COMMENT_LIKED:"+actorId+":"+commentId));});
    }
    public void notifyUserFollowed(Long actorId,Long recipientId){
        safely(()->enqueue(build(recipientId,actorId,NotificationType.USER_FOLLOWED,NotificationTargetType.USER,actorId,null,null,
                "新关注",actorName(actorId)+" 关注了你","USER_FOLLOWED:"+actorId+":"+recipientId)));
    }
    public void createSystemNotification(Long recipientId,String title,String content,String dedupKey){
        if(!StringUtils.hasText(dedupKey)) throw new BusinessException(ErrorCode.PARAM_ERROR,"dedupKey is required");
        SysUser recipient=sysUserMapper.selectById(recipientId);
        if(recipient==null||!ACTIVE.equals(recipient.getStatus())||!eq0(recipient.getIsDeleted())) throw new BusinessException(ErrorCode.NOT_FOUND,"recipient not found");
        safely(()->enqueue(build(recipientId,null,NotificationType.SYSTEM,NotificationTargetType.SYSTEM,null,null,null,
                title,content,"SYSTEM:"+dedupKey.trim())));
    }

    public PageResult<NotificationVO> list(long pageNum,long pageSize,String type,String readStatus){
        long pn=Math.max(1,pageNum),ps=Math.min(100,Math.max(1,pageSize));
        Long userId=CurrentUserHolder.get().getUserId();
        String resolvedType=StringUtils.hasText(type)?parseType(type).name():null;
        Integer readFlag=StringUtils.hasText(readStatus)?parseReadStatus(readStatus):null;
        long total=notificationMapper.countNotificationPage(userId,resolvedType,readFlag);
        List<Notification> rows=notificationMapper.selectNotificationPage(userId,resolvedType,readFlag,(pn-1)*ps,ps);
        return PageResult.of(toVOs(rows),total,pn,ps);
    }

    public UnreadCountVO unreadCount(){
        Long userId=CurrentUserHolder.get().getUserId();
        Long total=notificationMapper.selectCount(base(userId).eq(Notification::getReadFlag,0));
        return new UnreadCountVO(total==null?0:total,Map.of());
    }

    public boolean read(Long notificationId){
        Long userId=CurrentUserHolder.get().getUserId();LocalDateTime now=LocalDateTime.now();
        notificationMapper.update(null,new UpdateWrapper<Notification>().eq("id",notificationId).eq("recipient_user_id",userId)
                .eq("status",ACTIVE).eq("is_deleted",0).eq("read_flag",0).set("read_flag",1).set("read_time",now));
        return true;
    }
    public ReadAllVO readAll(){
        Long userId=CurrentUserHolder.get().getUserId();
        int count=notificationMapper.update(null,new UpdateWrapper<Notification>().eq("recipient_user_id",userId)
                .eq("status",ACTIVE).eq("is_deleted",0).eq("read_flag",0).set("read_flag",1).set("read_time",LocalDateTime.now()));
        return new ReadAllVO(count);
    }

    private List<NotificationVO> toVOs(List<Notification> rows){
        if(rows.isEmpty()) return List.of();
        Set<Long> actorIds=rows.stream().map(Notification::getActorUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long,UserProfile> profiles=actorIds.isEmpty()?Map.of():userProfileMapper.selectList(new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId,actorIds))
                .stream().collect(Collectors.toMap(UserProfile::getUserId,Function.identity(),(a,b)->a));
        Set<Long> contentIds=targetIds(rows,"CONTENT");Set<Long> commentIds=targetIds(rows,"COMMENT");
        Set<Long> userTargetIds=targetIds(rows,"USER");
        Map<Long,Content> contents=contentIds.isEmpty()?Map.of():contentMapper.selectBatchIds(contentIds).stream().collect(Collectors.toMap(Content::getId,Function.identity()));
        Map<Long,Comment> comments=commentIds.isEmpty()?Map.of():commentMapper.selectBatchIds(commentIds).stream().collect(Collectors.toMap(Comment::getId,Function.identity()));
        Map<Long,SysUser> users=userTargetIds.isEmpty()?Map.of():sysUserMapper.selectBatchIds(userTargetIds).stream().collect(Collectors.toMap(SysUser::getId,Function.identity()));
        List<NotificationVO> result=new ArrayList<>();
        for(Notification n:rows){
            UserProfile p=profiles.get(n.getActorUserId());
            NotificationVO.Actor actor=n.getActorUserId()==null?null:new NotificationVO.Actor(n.getActorUserId(),p==null?"用户":p.getNickname(),p==null?null:p.getAvatarUrl());
            boolean available=true;NotificationVO.TargetPreview preview=null;
            if("CONTENT".equals(n.getTargetType())){Content c=contents.get(n.getTargetId());available=c!=null&&eq0(c.getIsDeleted())&&"PUBLISHED".equals(c.getStatus());if(available)preview=new NotificationVO.TargetPreview(c.getTitle(),c.getCoverUrl(),null);}
            else if("COMMENT".equals(n.getTargetType())){Comment c=comments.get(n.getTargetId());available=c!=null&&eq0(c.getIsDeleted())&&ACTIVE.equals(c.getStatus());if(available)preview=new NotificationVO.TargetPreview(null,null,excerpt(c.getContentText()));}
            else if("USER".equals(n.getTargetType())) {SysUser u=users.get(n.getTargetId());available=u!=null&&ACTIVE.equals(u.getStatus())&&eq0(u.getIsDeleted());}
            result.add(new NotificationVO(n.getId(),n.getNotificationType(),actor,n.getTargetType(),n.getTargetId(),n.getSecondaryTargetType(),n.getSecondaryTargetId(),n.getTitle(),n.getContent(),Integer.valueOf(1).equals(n.getReadFlag()),n.getReadTime(),n.getCreateTime(),available,preview));
        }
        return result;
    }
    private Set<Long> targetIds(List<Notification> rows,String type){return rows.stream().filter(n->type.equals(n.getTargetType())).map(Notification::getTargetId).filter(Objects::nonNull).collect(Collectors.toSet());}
    private String actorName(Long id){UserProfile p=userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId,id).last("LIMIT 1"));return p==null||!StringUtils.hasText(p.getNickname())?"用户":p.getNickname();}
    private Notification build(Long recipient,Long actor,NotificationType type,NotificationTargetType targetType,Long targetId,NotificationTargetType secondaryType,Long secondaryId,String title,String content,String dedup){
        if(recipient==null||Objects.equals(recipient,actor)) return null;Notification n=new Notification();n.setRecipientUserId(recipient);n.setActorUserId(actor);n.setNotificationType(type.name());n.setTargetType(targetType==null?null:targetType.name());n.setTargetId(targetId);n.setSecondaryTargetType(secondaryType==null?null:secondaryType.name());n.setSecondaryTargetId(secondaryId);n.setTitle(title);n.setContent(content);n.setReadFlag(0);n.setStatus(ACTIVE);n.setDedupKey(dedup);n.setIsDeleted(0);n.setCreateTime(LocalDateTime.now());return n;
    }
    private void enqueue(Notification n){if(n==null)return;Runnable task=()->safely(()->writer.persist(n));if(TransactionSynchronizationManager.isActualTransactionActive()){TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){task.run();}});}else task.run();}
    private void safely(Runnable action){try{action.run();}catch(DuplicateKeyException ignored){log.debug("duplicate notification ignored");}catch(Exception e){log.warn("notification write skipped: {}",e.getMessage());}}
    private LambdaQueryWrapper<Notification> base(Long userId){return new LambdaQueryWrapper<Notification>().eq(Notification::getRecipientUserId,userId).eq(Notification::getStatus,ACTIVE).eq(Notification::getIsDeleted,0);}
    private NotificationType parseType(String v){try{return NotificationType.valueOf(v.trim().toUpperCase());}catch(Exception e){throw new BusinessException(ErrorCode.PARAM_ERROR,"unsupported notification type");}}
    private int parseReadStatus(String v){if("UNREAD".equalsIgnoreCase(v)||"0".equals(v))return 0;if("READ".equalsIgnoreCase(v)||"1".equals(v))return 1;throw new BusinessException(ErrorCode.PARAM_ERROR,"unsupported readStatus");}
    private static boolean eq0(Integer v){return v==null||v==0;} private static String excerpt(String s){return s==null?null:(s.length()<=80?s:s.substring(0,80));}
}
