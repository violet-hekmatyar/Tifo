package com.southstand.file.service;

import com.southstand.file.domain.StorageType;
import org.springframework.stereotype.Service;

@Service
public class QiniuKodoStorageService extends UnsupportedStorageService {

    @Override
    public StorageType storageType() {
        return StorageType.QINIU_KODO;
    }
}
