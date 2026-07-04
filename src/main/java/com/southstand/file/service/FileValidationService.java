package com.southstand.file.service;

import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.file.config.FileProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileValidationService {

    private final FileProperties fileProperties;

    public FileValidationService(FileProperties fileProperties) {
        this.fileProperties = fileProperties;
    }

    public ValidatedFile validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "empty file");
        }
        if (file.getSize() > fileProperties.getMaxSizeBytes()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "file size exceeds limit");
        }
        String originalName = safeOriginalName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!fileProperties.getAllowedExtensions().contains(extension)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "unsupported file extension");
        }
        String contentType = file.getContentType();
        if (contentType == null || contentType.trim().isEmpty()
                || !fileProperties.getAllowedContentTypes().contains(contentType.trim().toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "unsupported content type");
        }
        try {
            byte[] header = readHeader(file);
            if (!magicMatches(extension, contentType, header)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "file magic number mismatch");
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "file read failed");
        }
        return new ValidatedFile(originalName, extension, contentType.trim().toLowerCase(Locale.ROOT), file.getSize());
    }

    private String safeOriginalName(String originalFilename) {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "invalid file name");
        }
        String name = originalFilename.trim();
        if (name.length() > 255) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "file name too long");
        }
        if (name.contains("..") || name.contains("/") || name.contains("\\") || name.contains(":")
                || name.contains("*") || name.contains("?") || name.contains("\"") || name.contains("<")
                || name.contains(">") || name.contains("|")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "invalid file name");
        }
        return name;
    }

    private String extensionOf(String originalName) {
        int index = originalName.lastIndexOf('.');
        if (index < 0 || index == originalName.length() - 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "missing file extension");
        }
        return originalName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private byte[] readHeader(MultipartFile file) throws IOException {
        byte[] header = new byte[16];
        try (InputStream inputStream = file.getInputStream()) {
            int length = inputStream.read(header);
            if (length < 0) {
                return new byte[0];
            }
            if (length == header.length) {
                return header;
            }
            byte[] exact = new byte[length];
            System.arraycopy(header, 0, exact, 0, length);
            return exact;
        }
    }

    private boolean magicMatches(String extension, String contentType, byte[] header) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg".equals(contentType) && startsWith(header, 0xFF, 0xD8, 0xFF);
            case "png" -> "image/png".equals(contentType)
                    && startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "gif" -> "image/gif".equals(contentType) && startsWith(header, 0x47, 0x49, 0x46, 0x38);
            case "webp" -> "image/webp".equals(contentType)
                    && header.length >= 12
                    && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                    && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50;
            default -> false;
        };
    }

    private boolean startsWith(byte[] header, int... expected) {
        if (header.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((header[i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    public record ValidatedFile(String originalName, String extension, String contentType, long sizeBytes) {
    }
}
