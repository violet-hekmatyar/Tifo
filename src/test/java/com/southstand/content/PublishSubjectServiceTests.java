package com.southstand.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.content.entity.PublishSubject;
import com.southstand.content.mapper.PublishSubjectMapper;
import com.southstand.content.service.PublishSubjectService;
import com.southstand.content.vo.PublishSubjectVO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublishSubjectServiceTests {

    @Test
    void rejectsUnsupportedType() {
        PublishSubjectService service = new PublishSubjectService(mock(PublishSubjectMapper.class));
        assertThatThrownBy(() -> service.list("TEAM", null, 1L, 20L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARAM_ERROR);
    }

    @Test
    void mapsTopicAndCountsOnlyThroughMapper() {
        PublishSubjectMapper mapper = mock(PublishSubjectMapper.class);
        PublishSubject subject = new PublishSubject();
        subject.setId(16500000000000701L);
        subject.setSubjectType("TOPIC");
        subject.setName("英超焦点");
        subject.setSummary("本轮英超讨论");
        subject.setHotScore(BigDecimal.TEN);
        subject.setSortOrder(1);
        Page<PublishSubject> page = new Page<>(1, 20);
        page.setRecords(List.of(subject));
        page.setTotal(1);
        when(mapper.selectPage(any(), any())).thenReturn(page);
        when(mapper.countDiscussion("TOPIC", subject.getId())).thenReturn(3L);

        List<PublishSubjectVO> records = new PublishSubjectService(mapper)
                .list("topic", "英超", 1L, 20L).getRecords();

        assertThat(records).singleElement().satisfies(vo -> {
            assertThat(vo.getSubjectId()).isEqualTo(subject.getId());
            assertThat(vo.getSubjectType()).isEqualTo("TOPIC");
            assertThat(vo.getDiscussionCount()).isEqualTo(3L);
        });
    }
}
