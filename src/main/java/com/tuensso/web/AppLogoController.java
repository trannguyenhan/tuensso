package com.tuensso.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.tuensso.client.AppLogoStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/assets/app-logos")
public class AppLogoController {

    private final AppLogoStorageService appLogoStorageService;

    public AppLogoController(AppLogoStorageService appLogoStorageService) {
        this.appLogoStorageService = appLogoStorageService;
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> getLogo(@PathVariable String fileName) {
        Path path = appLogoStorageService.resolveForRead(fileName);
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Logo not found");
        }

        MediaType mediaType = detectMediaType(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(mediaType)
                .body(new FileSystemResource(path));
    }

    private static MediaType detectMediaType(Path path) {
        try {
            String type = Files.probeContentType(path);
            return type == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(type);
        } catch (IOException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}