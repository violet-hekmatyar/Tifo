package com.southstand.file.domain;

import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;

public enum FileBizType {
    AVATAR,
    CONTENT_IMAGE,
    COMMENT_IMAGE,
    GENERAL_IMAGE;

    public static FileBizType from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return GENERAL_IMAGE;
        }
        try {
            return FileBizType.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "unsupported bizType");
        }
    }
}
