package org.volumteerhub.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartException;
import org.volumteerhub.common.exception.BadRequestException;
import org.volumteerhub.dto.ErrorResponse;
import org.volumteerhub.service.StorageService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final StorageService storageService;

    public UploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping
    public ResponseEntity<?> uploadTempFile(@RequestParam("file") MultipartFile file) {
        try {
            UUID tempFileId = storageService.saveTempFile(file);

            return ResponseEntity.ok(Map.of("tempId", tempFileId));
        } catch (MultipartException e) {
            throw new BadRequestException("Invalid multipart request: " + e.getMessage());
        } catch (Exception e) {
            return new ResponseEntity<>(
                    ErrorResponse.build(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            e.getMessage(),
                            ""
                    ),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
