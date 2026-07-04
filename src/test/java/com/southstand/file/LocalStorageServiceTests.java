package com.southstand.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.southstand.file.config.FileProperties;
import com.southstand.file.domain.FileResourceEntity;
import com.southstand.file.service.LocalStorageService;
import com.southstand.file.service.StoreFileCommand;
import com.southstand.file.service.StoredFile;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalStorageServiceTests {

    @TempDir
    Path tempDir;

    @Test
    void storeCreatesRelativePathAndLoadReadsFile() throws Exception {
        LocalStorageService service = new LocalStorageService(properties());
        StoredFile stored = service.store(new StoreFileCommand(png(), "a.png", "image/png", "png", 8));

        assertThat(stored.relativePath()).doesNotContain("..");
        assertThat(stored.objectKey()).doesNotContain("..");
        assertThat(Files.walk(tempDir).filter(Files::isRegularFile).toList()).hasSize(1);

        FileResourceEntity entity = new FileResourceEntity();
        entity.setRelativePath(stored.relativePath());
        entity.setContentType("image/png");
        assertThat(service.load(entity).resource().exists()).isTrue();
    }

    @Test
    void deleteDoesNotPhysicallyRemoveFile() throws Exception {
        LocalStorageService service = new LocalStorageService(properties());
        StoredFile stored = service.store(new StoreFileCommand(png(), "a.png", "image/png", "png", 8));
        FileResourceEntity entity = new FileResourceEntity();
        entity.setRelativePath(stored.relativePath());

        service.delete(entity);

        assertThat(Files.walk(tempDir).filter(Files::isRegularFile).toList()).hasSize(1);
    }

    private FileProperties properties() {
        FileProperties properties = new FileProperties();
        properties.getLocal().setStorageRoot(tempDir.toString());
        return properties;
    }

    private MockMultipartFile png() {
        return new MockMultipartFile("file", "a.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
    }
}
