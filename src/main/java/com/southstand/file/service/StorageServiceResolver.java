package com.southstand.file.service;

import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.file.config.FileProperties;
import com.southstand.file.domain.FileResourceEntity;
import com.southstand.file.domain.StorageType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StorageServiceResolver {

    private final FileProperties fileProperties;
    private final Map<StorageType, StorageService> services = new EnumMap<>(StorageType.class);

    public StorageServiceResolver(FileProperties fileProperties, List<StorageService> storageServices) {
        this.fileProperties = fileProperties;
        for (StorageService service : storageServices) {
            services.put(service.storageType(), service);
        }
    }

    public StorageService current() {
        return resolve(StorageType.from(fileProperties.getStorageType()));
    }

    public StorageService resolve(FileResourceEntity fileResource) {
        return resolve(StorageType.from(fileResource.getStorageType()));
    }

    public StorageService resolve(StorageType storageType) {
        StorageService service = services.get(storageType);
        if (service == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "unknown storage type: " + storageType);
        }
        return service;
    }
}
