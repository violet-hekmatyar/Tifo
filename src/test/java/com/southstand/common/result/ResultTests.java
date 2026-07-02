package com.southstand.common.result;

import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ResultTests {

    @Test
    void successShouldUseUnifiedShape() {
        Result<String> result = Result.success("ok");

        assertEquals(0, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("ok", result.getData());
        assertNotNull(result.getTraceId());
    }

    @Test
    void failureShouldUseErrorCode() {
        Result<Void> result = Result.failure(ErrorCode.PARAM_ERROR);

        assertEquals(40001, result.getCode());
        assertEquals("参数错误", result.getMessage());
        assertNotNull(result.getTraceId());
    }

    @Test
    void pageResultShouldCalculatePages() {
        PageResult<String> page = PageResult.of(List.of("a", "b"), 21, 2, 10);

        assertEquals(2, page.getRecords().size());
        assertEquals(21, page.getTotal());
        assertEquals(2, page.getPageNum());
        assertEquals(10, page.getPageSize());
        assertEquals(3, page.getPages());
    }

    @Test
    void businessExceptionShouldCarryErrorCode() {
        BusinessException exception = new BusinessException(ErrorCode.NOT_FOUND);

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        assertEquals("资源不存在", exception.getMessage());
    }
}
