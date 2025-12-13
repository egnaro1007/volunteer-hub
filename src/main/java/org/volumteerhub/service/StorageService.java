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
    public UUID saveTempFile(MultipartFile file) throws IOException {
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

        return fileId;
    }

    /**
     * Move file from /temp to /uploads/{postId}/
     * @param tempFileId The UUID returned from saveTempFile
     * @param postId The UUID of the Post (destination folder)
     * @return The relative path to the new file (e.g., "uploads/uuid-post/file.jpg")
     */
    public String moveTempFileToPermanent(UUID tempFileId, UUID postId) throws IOException {
        // Find the file in temp (we need to find it because we don't know the extension)
        Path sourcePath;
        try (Stream<Path> stream = Files.list(props.getTempPath())) {
            sourcePath = stream
                    .filter(p -> p.getFileName().toString().startsWith(tempFileId.toString()))
                    .findFirst()
                    .orElseThrow(() -> new IOException("Temp file not found: " + tempFileId));
        }

        // reate the destination folder: /uploads/{postId}/
        Path postDirPath = props.getUploadsPath().resolve(postId.toString());
        if (!Files.exists(postDirPath)) {
            Files.createDirectories(postDirPath);
        }

        // Move the file
        String filename = sourcePath.getFileName().toString();
        Path destinationPath = postDirPath.resolve(filename);

        Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + postId + "/" + filename;
    }
}
