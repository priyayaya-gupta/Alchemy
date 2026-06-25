package com.example.alchemy.Controller;

import com.example.alchemy.Service.FileProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {
    private static final Logger log =
            LoggerFactory.getLogger(FileUploadController.class);
    private final FileProcessingService service;

    public FileUploadController(FileProcessingService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("files") MultipartFile[] files) {

        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body("No files uploaded");
        }

        int processed = 0;

        for (MultipartFile file : files) {
            try {
                if (!file.isEmpty()) {
                    service.process(file);
                    processed++;
                }
            } catch (Exception e) {
                return ResponseEntity
                        .status(500)
                        .body("Error processing file: " + file.getOriginalFilename());
            }
        }

        return ResponseEntity.ok("Processed " + processed + " files successfully");
    }

}