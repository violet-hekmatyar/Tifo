package com.southstand.file.service;

import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.file.domain.FileResourceEntity;
import com.southstand.file.domain.StorageType;

public abstract class UnsupportedStorageService implements StorageService {

    @Override
    public StoredFile store(StoreFileCommand command) {
        throw unsupported();
    }

    @Override
    public FileLoadResult load(FileResourceEntity fileResource) {
        throw unsupported();
    }

    @Override
    public void delete(FileResourceEntity fileResource) {
        throw unsupported();
    }

    protected BusinessException unsupported() {
        return new BusinessException(ErrorCode.SYSTEM_ERROR, "current storage type is not implemented: " + storageType());
    }
}
