package com.southstand.file.service;

import org.springframework.core.io.Resource;

public record FileLoadResult(Resource resource, String contentType) {
}
