package com.southstand.file.service;

import com.southstand.file.domain.StorageType;

public record StoredFile(
        StorageType storageType,
        String bucket,
        String endpoint,
        String publicDomain,
        String objectKey,
        String relativePath,
        String storageName,
        String url,
        String contentType,
        String extension,
        long sizeBytes,
        String etag
) {
}
