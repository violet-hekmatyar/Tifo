package com.southstand.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.southstand.notification.entity.Notification;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
    @Select("<script>SELECT id,recipient_user_id,actor_user_id,notification_type,target_type,target_id,secondary_target_type,secondary_target_id,title,content,read_flag,read_time,status,dedup_key,create_time,update_time,is_deleted FROM notification WHERE recipient_user_id=#{recipientId} AND status='ACTIVE' AND is_deleted=0 <if test='type != null'>AND notification_type=#{type}</if> <if test='readFlag != null'>AND read_flag=#{readFlag}</if> ORDER BY create_time DESC,id DESC LIMIT #{offset},#{pageSize}</script>")
    List<Notification> selectNotificationPage(@Param("recipientId") Long recipientId,@Param("type") String type,
            @Param("readFlag") Integer readFlag,@Param("offset") long offset,@Param("pageSize") long pageSize);

    @Select("<script>SELECT COUNT(*) FROM notification WHERE recipient_user_id=#{recipientId} AND status='ACTIVE' AND is_deleted=0 <if test='type != null'>AND notification_type=#{type}</if> <if test='readFlag != null'>AND read_flag=#{readFlag}</if></script>")
    long countNotificationPage(@Param("recipientId") Long recipientId,@Param("type") String type,@Param("readFlag") Integer readFlag);
}
