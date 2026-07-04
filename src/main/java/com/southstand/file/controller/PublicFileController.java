package com.southstand.file.controller;

import com.southstand.file.service.FileStorageService;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/files")
public class PublicFileController {

    private final FileStorageService fileStorageService;

    public PublicFileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> get(@PathVariable Long fileId) {
        Optional<FileStorageService.StoredPublicFile> storedFile = fileStorageService.findPublicFile(fileId);
        if (storedFile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(storedFile.get().contentType()))
                .cacheControl(CacheControl.noStore())
                .body(storedFile.get().resource());
    }
}
