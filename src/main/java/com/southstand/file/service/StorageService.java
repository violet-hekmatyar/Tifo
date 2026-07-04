package com.southstand.file.service;

import com.southstand.file.domain.FileResourceEntity;
import com.southstand.file.domain.StorageType;

public interface StorageService {

    StorageType storageType();

    StoredFile store(StoreFileCommand command);

    FileLoadResult load(FileResourceEntity fileResource);

    void delete(FileResourceEntity fileResource);
}
