package com.southstand.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.southstand.common.exception.BusinessException;
import com.southstand.file.config.FileProperties;
import com.southstand.file.domain.FileResourceEntity;
import com.southstand.file.domain.StorageType;
import com.southstand.file.service.AliyunOssStorageService;
import com.southstand.file.service.LocalStorageService;
import com.southstand.file.service.StorageServiceResolver;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StorageServiceResolverTests {

    @TempDir
    Path tempDir;

    @Test
    void defaultLocalResolvesToLocalStorageService() {
        FileProperties properties = properties();
        StorageServiceResolver resolver = new StorageServiceResolver(properties, List.of(new LocalStorageService(properties)));

        assertThat(resolver.current().storageType()).isEqualTo(StorageType.LOCAL);
    }

    @Test
    void fileResourceStorageTypeResolvesToLocal() {
        FileProperties properties = properties();
        StorageServiceResolver resolver = new StorageServiceResolver(properties, List.of(new LocalStorageService(properties)));
        FileResourceEntity file = new FileResourceEntity();
        file.setStorageType("LOCAL");

        assertThat(resolver.resolve(file).storageType()).isEqualTo(StorageType.LOCAL);
    }

    @Test
    void unsupportedStorageHasClearErrorWhenCalled() {
        FileProperties properties = properties();
        properties.setStorageType("ALIYUN_OSS");
        StorageServiceResolver resolver = new StorageServiceResolver(properties, List.of(new AliyunOssStorageService()));

        assertThatThrownBy(() -> resolver.current().store(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ALIYUN_OSS");
    }

    @Test
    void unknownStorageTypeHasClearError() {
        FileProperties properties = properties();
        properties.setStorageType("UNKNOWN");
        StorageServiceResolver resolver = new StorageServiceResolver(properties, List.of());

        assertThatThrownBy(resolver::current)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("UNKNOWN");
    }

    private FileProperties properties() {
        FileProperties properties = new FileProperties();
        properties.getLocal().setStorageRoot(tempDir.toString());
        return properties;
    }
}
