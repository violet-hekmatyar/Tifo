package com.southstand.football.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.southstand.football.event.entity.MatchEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MatchEventMapper extends BaseMapper<MatchEvent> {
}
