package com.southstand.file.service;

import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.file.config.FileProperties;
import com.southstand.file.domain.FileBizType;
import com.southstand.file.domain.FileResourceEntity;
import com.southstand.file.domain.FileStatus;
import com.southstand.file.mapper.FileResourceMapper;
import com.southstand.file.vo.FileUploadVO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final FileProperties fileProperties;
    private final FileValidationService fileValidationService;
    private final FileResourceMapper fileResourceMapper;

    public FileStorageService(FileProperties fileProperties,
            FileValidationService fileValidationService,
            FileResourceMapper fileResourceMapper) {
        this.fileProperties = fileProperties;
        this.fileValidationService = fileValidationService;
        this.fileResourceMapper = fileResourceMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public FileUploadVO upload(MultipartFile file, String bizTypeValue) {
        FileBizType bizType = FileBizType.from(bizTypeValue);
        FileValidationService.ValidatedFile validated = fileValidationService.validate(file);
        Long userId = CurrentUserHolder.get().getUserId();

        LocalDate today = LocalDate.now();
        String datePath = "%04d/%02d/%02d".formatted(today.getYear(), today.getMonthValue(), today.getDayOfMonth());
        String storageName = UUID.randomUUID() + "." + validated.extension();
        String relativePath = datePath + "/" + storageName;
        String objectKey = relativePath;
        Path root = storageRoot();
        Path target = root.resolve(relativePath).normalize().toAbsolutePath();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "invalid file path");
        }

        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "file save failed");
        }

        FileResourceEntity entity = new FileResourceEntity();
        entity.setUserId(userId);
        entity.setBizType(bizType.name());
        entity.setOriginalName(validated.originalName());
        entity.setStorageName(storageName);
        entity.setObjectKey(objectKey);
        entity.setRelativePath(relativePath);
        entity.setUrl(fileProperties.getPublicUrlPrefix() + "/pending");
        entity.setContentType(validated.contentType());
        entity.setExtension(validated.extension());
        entity.setSizeBytes(validated.sizeBytes());
        entity.setStatus(FileStatus.ACTIVE);
        entity.setDeleted(0);
        fileResourceMapper.insert(entity);
        String url = fileProperties.getPublicUrlPrefix() + "/" + entity.getId();
        entity.setUrl(url);
        fileResourceMapper.updateById(entity);

        FileUploadVO vo = new FileUploadVO();
        vo.setFileId(entity.getId());
        vo.setUrl(url);
        vo.setBizType(entity.getBizType());
        vo.setOriginalName(entity.getOriginalName());
        vo.setContentType(entity.getContentType());
        vo.setExtension(entity.getExtension());
        vo.setSizeBytes(entity.getSizeBytes());
        return vo;
    }

    public Optional<StoredFile> findPublicFile(Long fileId) {
        FileResourceEntity entity = fileResourceMapper.selectById(fileId);
        if (entity == null || Integer.valueOf(1).equals(entity.getDeleted()) || !FileStatus.ACTIVE.equals(entity.getStatus())) {
            return Optional.empty();
        }
        Path root = storageRoot();
        Path target = root.resolve(entity.getRelativePath()).normalize().toAbsolutePath();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            return Optional.empty();
        }
        return Optional.of(new StoredFile(new PathResource(target), entity.getContentType()));
    }

    public Path storageRoot() {
        try {
            Path root = Path.of(fileProperties.getStorageRoot()).normalize().toAbsolutePath();
            Files.createDirectories(root);
            return root;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "file storage unavailable");
        }
    }

    public record StoredFile(Resource resource, String contentType) {
    }
}
