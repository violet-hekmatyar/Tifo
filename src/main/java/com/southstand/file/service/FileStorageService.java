package com.southstand.file.service;

import com.southstand.auth.security.CurrentUserHolder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.file.config.FileProperties;
import com.southstand.file.domain.FileBizType;
import com.southstand.file.domain.FileResourceEntity;
import com.southstand.file.domain.FileStatus;
import com.southstand.file.mapper.FileResourceMapper;
import com.southstand.file.vo.FileUploadVO;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final FileProperties fileProperties;
    private final FileValidationService fileValidationService;
    private final FileResourceMapper fileResourceMapper;
    private final StorageServiceResolver storageServiceResolver;

    public FileStorageService(FileProperties fileProperties,
            FileValidationService fileValidationService,
            FileResourceMapper fileResourceMapper,
            StorageServiceResolver storageServiceResolver) {
        this.fileProperties = fileProperties;
        this.fileValidationService = fileValidationService;
        this.fileResourceMapper = fileResourceMapper;
        this.storageServiceResolver = storageServiceResolver;
    }

    @Transactional(rollbackFor = Exception.class)
    public FileUploadVO upload(MultipartFile file, String bizTypeValue) {
        FileBizType bizType = FileBizType.from(bizTypeValue);
        FileValidationService.ValidatedFile validated = fileValidationService.validate(file);
        Long userId = CurrentUserHolder.get().getUserId();
        StoredFile storedFile = storageServiceResolver.current().store(new StoreFileCommand(
                file,
                validated.originalName(),
                validated.contentType(),
                validated.extension(),
                validated.sizeBytes()
        ));

        FileResourceEntity entity = new FileResourceEntity();
        entity.setUserId(userId);
        entity.setBizType(bizType.name());
        entity.setOriginalName(validated.originalName());
        entity.setStorageName(storedFile.storageName());
        entity.setObjectKey(storedFile.objectKey());
        entity.setRelativePath(storedFile.relativePath());
        entity.setUrl(fileProperties.getPublicUrlPrefix() + "/pending");
        entity.setContentType(validated.contentType());
        entity.setExtension(validated.extension());
        entity.setSizeBytes(validated.sizeBytes());
        entity.setStorageType(storedFile.storageType().name());
        entity.setBucket(storedFile.bucket());
        entity.setEndpoint(storedFile.endpoint());
        entity.setPublicDomain(storedFile.publicDomain());
        entity.setEtag(storedFile.etag());
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
        vo.setStorageType(entity.getStorageType());
        vo.setObjectKey(entity.getObjectKey());
        return vo;
    }

    public Optional<StoredPublicFile> findPublicFile(Long fileId) {
        FileResourceEntity entity = fileResourceMapper.selectById(fileId);
        if (entity == null || Integer.valueOf(1).equals(entity.getDeleted()) || !FileStatus.ACTIVE.equals(entity.getStatus())) {
            return Optional.empty();
        }
        try {
            FileLoadResult result = storageServiceResolver.resolve(entity).load(entity);
            return Optional.of(new StoredPublicFile(result.resource(), result.contentType()));
        } catch (BusinessException ex) {
            return Optional.empty();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean softDelete(Long fileId) {
        Long userId = CurrentUserHolder.get().getUserId();
        FileResourceEntity entity = requireActiveFile(fileId);
        if (!userId.equals(entity.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "file does not belong to current user");
        }
        storageServiceResolver.resolve(entity).delete(entity);
        fileResourceMapper.update(null, new LambdaUpdateWrapper<FileResourceEntity>()
                .eq(FileResourceEntity::getId, fileId)
                .set(FileResourceEntity::getStatus, FileStatus.DELETED)
                .set(FileResourceEntity::getDeleted, 1));
        return true;
    }

    public FileResourceEntity requireActiveFile(Long fileId) {
        FileResourceEntity entity = fileResourceMapper.selectById(fileId);
        if (entity == null || Integer.valueOf(1).equals(entity.getDeleted()) || !FileStatus.ACTIVE.equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "file not found");
        }
        return entity;
    }

    public record StoredPublicFile(Resource resource, String contentType) {
    }
}
