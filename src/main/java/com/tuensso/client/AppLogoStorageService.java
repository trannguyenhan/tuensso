package com.tuensso.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppLogoStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");

    private final Path logoDir;

    public AppLogoStorageService(@Value("${tuensso.logo-upload-dir:./data/app-logos}") String logoDir) {
        this.logoDir = Path.of(logoDir).toAbsolutePath().normalize();
    }

    public String store(String clientId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logo file is required");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported logo type. Allowed: png, jpg, jpeg, webp");
        }

        String safeClientId = clientId.replaceAll("[^a-zA-Z0-9_-]", "-").toLowerCase(Locale.ROOT);
        String fileName = safeClientId + "." + extension;
        Path target = logoDir.resolve(fileName).normalize();

        if (!target.startsWith(logoDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid path");
        }

        try {
            Files.createDirectories(logoDir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot store logo", ex);
        }

        return "/assets/app-logos/" + fileName;
    }

    public Path resolveForRead(String fileName) {
        Path path = logoDir.resolve(fileName).normalize();
        if (!path.startsWith(logoDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid logo path");
        }
        return path;
    }

    private static String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}