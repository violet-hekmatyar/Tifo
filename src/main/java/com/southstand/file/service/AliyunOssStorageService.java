package com.southstand.file.service;

import com.southstand.file.domain.StorageType;
import org.springframework.stereotype.Service;

@Service
public class AliyunOssStorageService extends UnsupportedStorageService {

    @Override
    public StorageType storageType() {
        return StorageType.ALIYUN_OSS;
    }
}
