package com.southstand.file.service;

import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.file.config.FileProperties;
import com.southstand.file.domain.FileResourceEntity;
import com.southstand.file.domain.StorageType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Service;

@Service
public class LocalStorageService implements StorageService {

    private final FileProperties fileProperties;

    public LocalStorageService(FileProperties fileProperties) {
        this.fileProperties = fileProperties;
    }

    @Override
    public StorageType storageType() {
        return StorageType.LOCAL;
    }

    @Override
    public StoredFile store(StoreFileCommand command) {
        LocalDate today = LocalDate.now();
        String datePath = "%04d/%02d/%02d".formatted(today.getYear(), today.getMonthValue(), today.getDayOfMonth());
        String storageName = UUID.randomUUID() + "." + command.extension();
        String relativePath = datePath + "/" + storageName;
        Path root = storageRoot();
        Path target = root.resolve(relativePath).normalize().toAbsolutePath();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "invalid file path");
        }
        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = command.file().getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "file save failed");
        }
        return new StoredFile(
                StorageType.LOCAL,
                null,
                null,
                null,
                relativePath,
                relativePath,
                storageName,
                null,
                command.contentType(),
                command.extension(),
                command.sizeBytes(),
                null
        );
    }

    @Override
    public FileLoadResult load(FileResourceEntity fileResource) {
        Path root = storageRoot();
        Path target = root.resolve(fileResource.getRelativePath()).normalize().toAbsolutePath();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "file not found");
        }
        return new FileLoadResult(new PathResource(target), fileResource.getContentType());
    }

    @Override
    public void delete(FileResourceEntity fileResource) {
        // T10 only soft-deletes metadata. The physical file is retained for later cleanup.
    }

    public Path storageRoot() {
        try {
            Path root = Path.of(fileProperties.getLocal().getStorageRoot()).normalize().toAbsolutePath();
            Files.createDirectories(root);
            return root;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "file storage unavailable");
        }
    }
}
