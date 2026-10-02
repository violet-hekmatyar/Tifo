package com.southstand.content.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.common.result.PageResult;
import com.southstand.content.entity.PublishSubject;
import com.southstand.content.mapper.PublishSubjectMapper;
import com.southstand.content.vo.PublishSubjectVO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PublishSubjectService {

    private static final String ACTIVE = "ACTIVE";
    private static final int NOT_DELETED = 0;

    private final PublishSubjectMapper mapper;

    public PublishSubjectService(PublishSubjectMapper mapper) {
        this.mapper = mapper;
    }

    public PageResult<PublishSubjectVO> list(String subjectType, String keyword, Long pageNum, Long pageSize) {
        String type = normalizeType(subjectType);
        long pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long ps = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 50);
        String kw = keyword == null ? "" : keyword.trim();
        QueryWrapper<PublishSubject> query = new QueryWrapper<PublishSubject>()
                .eq("subject_type", type)
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .orderByDesc("hot_score")
                .orderByAsc("sort_order")
                .orderByAsc("id");
        if (!kw.isEmpty()) {
            query.and(w -> w.like("name", kw).or().like("summary", kw));
        }
        Page<PublishSubject> page = mapper.selectPage(new Page<>(pn, ps), query);
        long total = page.getTotal();
        if (total == 0 && !page.getRecords().isEmpty()) {
            total = mapper.selectCount(new QueryWrapper<PublishSubject>()
                    .eq("subject_type", type)
                    .eq("status", ACTIVE)
                    .eq("is_deleted", NOT_DELETED)
                    .and(!kw.isEmpty(), w -> w.like("name", kw).or().like("summary", kw)));
        }
        List<PublishSubjectVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, total, page.getCurrent(), page.getSize());
    }

    public PublishSubject active(String subjectType, Long subjectId) {
        String type = normalizeType(subjectType);
        if (subjectId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "publish subject not found");
        }
        PublishSubject subject = mapper.selectOne(new QueryWrapper<PublishSubject>()
                .eq("id", subjectId)
                .eq("subject_type", type)
                .eq("status", ACTIVE)
                .eq("is_deleted", NOT_DELETED)
                .last("LIMIT 1"));
        if (subject == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "publish subject not found");
        }
        return subject;
    }

    public String normalizeType(String subjectType) {
        String type = subjectType == null ? "" : subjectType.trim().toUpperCase();
        if (!"TOPIC".equals(type) && !"HOT_EVENT".equals(type)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "unsupported publish subject type");
        }
        return type;
    }

    private PublishSubjectVO toVO(PublishSubject subject) {
        PublishSubjectVO vo = new PublishSubjectVO();
        vo.setSubjectId(subject.getId());
        vo.setSubjectType(subject.getSubjectType());
        vo.setName(subject.getName());
        vo.setSummary(subject.getSummary());
        vo.setCoverUrl(subject.getCoverUrl());
        vo.setDiscussionCount(mapper.countDiscussion(subject.getSubjectType(), subject.getId()));
        return vo;
    }
}
