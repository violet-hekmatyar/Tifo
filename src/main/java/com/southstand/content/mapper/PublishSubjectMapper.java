package com.southstand.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.southstand.content.entity.PublishSubject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PublishSubjectMapper extends BaseMapper<PublishSubject> {

    @Select("SELECT COUNT(DISTINCT r.content_id) "
            + "FROM content_relation r JOIN content c ON c.id=r.content_id "
            + "WHERE r.relation_type=#{subjectType} AND r.relation_id=#{subjectId} "
            + "AND r.status='ACTIVE' AND r.is_deleted=0 "
            + "AND c.status IN ('ACTIVE','PUBLISHED') AND c.is_deleted=0")
    long countDiscussion(@Param("subjectType") String subjectType, @Param("subjectId") Long subjectId);
}
