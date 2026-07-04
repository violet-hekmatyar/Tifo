package com.southstand.file.domain;

import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;

public enum StorageType {
    LOCAL,
    ALIYUN_OSS,
    QINIU_KODO,
    MINIO;

    public static StorageType from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LOCAL;
        }
        try {
            return StorageType.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "unknown storage type: " + value);
        }
    }
}
