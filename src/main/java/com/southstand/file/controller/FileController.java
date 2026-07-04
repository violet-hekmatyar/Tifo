package com.southstand.file.controller;

import com.southstand.common.result.Result;
import com.southstand.file.service.FileStorageService;
import com.southstand.file.vo.FileUploadVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/app/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    public Result<FileUploadVO> upload(@RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "GENERAL_IMAGE") String bizType) {
        return Result.success(fileStorageService.upload(file, bizType));
    }
}
