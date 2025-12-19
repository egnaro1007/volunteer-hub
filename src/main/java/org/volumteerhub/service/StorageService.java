package org.volumteerhub.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.volumteerhub.config.StorageProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class StorageService {

    private final StorageProperties props;

    public StorageService(StorageProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(props.getPublicPath());
            Files.createDirectories(props.getUploadsPath());
            Files.createDirectories(props.getTempPath());
            System.out.println("Storage initialized at: " + props.getRootPath());
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage folders!", e);
        }
    }

    /**
     * Save file to /temp folder
     * @return The UUID of the temporary file
     */
    public String saveTempFile(MultipartFile file) throws IOException {
        UUID fileId = UUID.randomUUID();

        // Extract extension (e.g. .jpg) to keep the filename valid
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Filename format: {UUID}.{ext}
        String newFilename = fileId + extension;
        Path tempFilePath = props.getTempPath().resolve(newFilename);

        Files.copy(file.getInputStream(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        return newFilename;
    }

    public String moveTempFileToPermanent(String tempFileName, UUID eventId, UUID postId) throws IOException {
        String subPath = eventId.toString() + "/" + postId.toString();
        return processMove(tempFileName, subPath);
    }

    public String moveTempFileToPermanent(String tempFileName, UUID eventId) throws IOException {
        String subPath = eventId.toString() + "/banner";
        return processMove(tempFileName, subPath);
    }

    /**
     * Handle the file system operations
     */
    private String processMove(String filename, String subPath) throws IOException {
        if (!filename.contains(".")) {
            throw new IOException("Temp file not found: " + filename);
        }

        Path sourcePath;
        try (Stream<Path> stream = Files.list(props.getTempPath())) {
            sourcePath = stream
                    .filter(p -> p.getFileName().toString().startsWith(filename))
                    .findFirst()
                    .orElseThrow(() -> new IOException("Temp file not found: " + filename));
        }

        // Create the destination folder: /uploads/{subPath}
        Path destDirPath = props.getUploadsPath().resolve(subPath);
        if (!Files.exists(destDirPath)) {
            Files.createDirectories(destDirPath);
        }

        // Move the file
        Path destinationPath = destDirPath.resolve(filename);
        Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + subPath + "/" + filename;
    }
}
