package com.bugtracker.service;

import com.bugtracker.config.AppProperties;
import com.bugtracker.exception.FileStorageException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores bug evidence on the local filesystem using generated, safe file names.
 *
 * <p>Only a whitelist of image / document / log extensions is accepted, the
 * declared content type must match an allowed prefix and every path is resolved
 * inside the configured upload directory to prevent directory traversal.</p>
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB

    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "png", "jpg", "jpeg", "gif", "webp", "bmp",
            "pdf", "txt", "log", "json", "csv", "md", "rtf",
            "doc", "docx", "xls", "xlsx");

    private static final List<String> ALLOWED_CONTENT_TYPE_PREFIXES = List.of(
            "image/png", "image/jpeg", "image/gif", "image/webp", "image/bmp",
            "application/pdf", "text/plain", "text/csv", "application/json",
            "application/msword", "application/vnd.openxmlformats-officedocument",
            "application/vnd.ms-excel", "application/rtf");

    /** Extensions that must never be accepted, even when renamed. */
    private static final List<String> FORBIDDEN_EXTENSIONS = List.of(
            "exe", "dll", "bat", "cmd", "sh", "js", "jar", "msi", "com", "scr",
            "ps1", "vbs", "php", "jsp", "py", "rb", "pl", "apk", "dmg", "html", "htm");

    private final Path uploadRoot;

    public FileStorageService(AppProperties appProperties) {
        this.uploadRoot = Paths.get(appProperties.getUploadDir()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(uploadRoot);
            log.info("Attachment upload directory ready at {}", uploadRoot);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create the upload directory: " + uploadRoot, ex);
        }
    }

    /**
     * Validates and stores the file.
     *
     * @return the generated (safe) file name written to disk
     */
    public String store(MultipartFile file) {
        validate(file);
        String extension = extractExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "." + extension;
        try {
            Path target = uploadRoot.resolve(storedName).normalize();
            if (!target.startsWith(uploadRoot)) {
                throw new FileStorageException("Invalid file path detected.");
            }
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return storedName;
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store the uploaded file.", ex);
        }
    }

    public Resource loadAsResource(String storedFileName) {
        try {
            Path filePath = resolveSafely(storedFileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileStorageException("Stored attachment could not be read: " + storedFileName);
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new FileStorageException("Stored attachment could not be read: " + storedFileName, ex);
        }
    }

    public void delete(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafely(storedFileName));
        } catch (IOException ex) {
            log.warn("Could not delete stored attachment {}: {}", storedFileName, ex.getMessage());
        }
    }

    public static long getMaxFileSizeBytes() {
        return MAX_FILE_SIZE_BYTES;
    }

    /**
     * Validates size, extension and declared content type of an upload.
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Please choose a file to upload.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new FileStorageException("File is too large. The maximum allowed size is 5 MB.");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new FileStorageException("The uploaded file must have a name.");
        }
        String extension = extractExtension(originalName);
        if (FORBIDDEN_EXTENSIONS.contains(extension)) {
            throw new FileStorageException("Files of type '." + extension + "' are not allowed.");
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new FileStorageException("Unsupported file type '." + extension + "'. Allowed types: "
                    + String.join(", ", ALLOWED_EXTENSIONS) + ".");
        }
        String contentType = file.getContentType();
        if (contentType == null
                || ALLOWED_CONTENT_TYPE_PREFIXES.stream().noneMatch(contentType::startsWith)) {
            throw new FileStorageException("Unsupported content type: " + contentType + ".");
        }
    }

    private Path resolveSafely(String storedFileName) {
        Path filePath = uploadRoot.resolve(storedFileName).normalize();
        if (!filePath.startsWith(uploadRoot)) {
            throw new FileStorageException("Invalid file path detected.");
        }
        return filePath;
    }

    private static String extractExtension(String fileName) {
        String cleaned = StringUtils.cleanPath(fileName == null ? "" : fileName);
        int dotIndex = cleaned.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == cleaned.length() - 1) {
            throw new FileStorageException("The uploaded file must have a valid extension.");
        }
        return cleaned.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
