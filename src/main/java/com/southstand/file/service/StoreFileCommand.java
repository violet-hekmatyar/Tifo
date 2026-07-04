package com.southstand.file.service;

import org.springframework.web.multipart.MultipartFile;

public record StoreFileCommand(
        MultipartFile file,
        String originalName,
        String contentType,
        String extension,
        long sizeBytes
) {
}
