package com.southstand.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.file.config.FileProperties;
import com.southstand.file.service.FileValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileValidationServiceTests {

    private final FileValidationService service = new FileValidationService(properties());

    @Test
    void allowsImageTypesWithMatchingMagic() {
        assertThat(service.validate(file("a.jpg", "image/jpeg", bytes(0xFF, 0xD8, 0xFF))).extension()).isEqualTo("jpg");
        assertThat(service.validate(file("a.png", "image/png", bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))).extension()).isEqualTo("png");
        assertThat(service.validate(file("a.gif", "image/gif", bytes(0x47, 0x49, 0x46, 0x38))).extension()).isEqualTo("gif");
        assertThat(service.validate(file("a.webp", "image/webp", bytes(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50))).extension()).isEqualTo("webp");
    }

    @Test
    void rejectsUnsupportedExtensions() {
        for (String name : new String[]{"a.txt", "a.svg", "a.js", "a.exe", "a.sh", "a.jar"}) {
            assertParamError(() -> service.validate(file(name, "image/png", bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))));
        }
    }

    @Test
    void rejectsEmptyOversizeBlankContentTypeAndWrongMagic() {
        assertParamError(() -> service.validate(new MockMultipartFile("file", "a.png", "image/png", new byte[0])));
        assertParamError(() -> service.validate(new MockMultipartFile("file", "a.png", "image/png", new byte[21])));
        assertParamError(() -> service.validate(file("a.png", null, bytes(0x89, 0x50, 0x4E, 0x47))));
        assertParamError(() -> service.validate(file("a.png", "text/plain", bytes(0x89, 0x50, 0x4E, 0x47))));
        assertParamError(() -> service.validate(file("a.png", "image/png", bytes(0xFF, 0xD8, 0xFF))));
    }

    @Test
    void rejectsDangerousFileNames() {
        for (String name : new String[]{"../a.png", "a/b.png", "a\\b.png", "a:b.png", "a?.png", "a|.png"}) {
            assertParamError(() -> service.validate(file(name, "image/png", bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))));
        }
    }

    private void assertParamError(ThrowingRunnable runnable) {
        assertThatThrownBy(runnable::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.PARAM_ERROR));
    }

    private MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    private byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }

    private FileProperties properties() {
        FileProperties properties = new FileProperties();
        properties.setMaxSizeBytes(20);
        return properties;
    }

    private interface ThrowingRunnable {
        void run();
    }
}
