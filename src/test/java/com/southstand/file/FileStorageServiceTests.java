package com.southstand.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.file.config.FileProperties;
import com.southstand.file.domain.FileResourceEntity;
import com.southstand.file.mapper.FileResourceMapper;
import com.southstand.file.service.FileStorageService;
import com.southstand.file.service.FileValidationService;
import com.southstand.file.service.LocalStorageService;
import com.southstand.file.service.StorageServiceResolver;
import com.southstand.file.vo.FileUploadVO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTests {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    void savesFileUnderStorageRootWithGeneratedName() throws Exception {
        CurrentUserHolder.set(new LoginUserContext(10002L, "user", "USER"));
        FileResourceMapper mapper = mock(FileResourceMapper.class);
        when(mapper.insert(any(FileResourceEntity.class))).then((Answer<Integer>) invocation -> {
            FileResourceEntity entity = invocation.getArgument(0);
            entity.setId(90001L);
            return 1;
        });
        FileStorageService service = service(mapper);

        FileUploadVO vo = service.upload(png("avatar.png"), "AVATAR");

        assertThat(vo.getFileId()).isEqualTo(90001L);
        assertThat(vo.getUrl()).isEqualTo("/api/public/files/90001");
        assertThat(Files.walk(tempDir).filter(Files::isRegularFile).toList()).hasSize(1);
        Path saved = Files.walk(tempDir).filter(Files::isRegularFile).findFirst().orElseThrow();
        assertThat(saved.getFileName().toString()).isNotEqualTo("avatar.png");
        assertThat(saved.normalize().toAbsolutePath().startsWith(tempDir.normalize().toAbsolutePath())).isTrue();
    }

    @Test
    void publicReadRejectsPathTraversal() {
        FileResourceEntity entity = new FileResourceEntity();
        entity.setId(1L);
        entity.setRelativePath("../evil.png");
        entity.setStorageType("LOCAL");
        entity.setContentType("image/png");
        entity.setStatus("ACTIVE");
        entity.setDeleted(0);
        FileResourceMapper mapper = mock(FileResourceMapper.class);
        when(mapper.selectById(1L)).thenReturn(entity);

        assertThat(service(mapper).findPublicFile(1L)).isEmpty();
    }

    private FileStorageService service(FileResourceMapper mapper) {
        FileProperties properties = new FileProperties();
        properties.getLocal().setStorageRoot(tempDir.toString());
        FileValidationService validationService = new FileValidationService(properties);
        LocalStorageService localStorageService = new LocalStorageService(properties);
        StorageServiceResolver resolver = new StorageServiceResolver(properties, List.of(localStorageService));
        return new FileStorageService(properties, validationService, mapper, resolver);
    }

    private MockMultipartFile png(String name) {
        return new MockMultipartFile("file", name, "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
    }
}
