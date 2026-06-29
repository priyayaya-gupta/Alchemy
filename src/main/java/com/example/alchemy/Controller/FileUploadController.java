package com.example.alchemy.Controller;

import com.example.alchemy.Service.FileProcessingService;
import com.example.alchemy.Service.QdrantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    private final FileProcessingService service;
    private final QdrantService qdrantService;

    public FileUploadController(FileProcessingService service,
            QdrantService qdrantService) {
        this.service = service;
        this.qdrantService = qdrantService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("files") MultipartFile[] files) {

        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body("No files uploaded");
        }

        List<Map<String, String>> uploadedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                if (!file.isEmpty()) {
                    String documentId = service.process(file);

                    uploadedFiles.add(
                            Map.of(
                                    "documentId", documentId,
                                    "fileName", file.getOriginalFilename()));
                }
            } catch (Exception e) {
                log.error("Error uploading {}", file.getOriginalFilename(), e);

                return ResponseEntity.status(500)
                        .body(e.getMessage());
            }
        }

        return ResponseEntity.ok(uploadedFiles);
    }

    @GetMapping("/files")
    public List<Map<String, String>> getFiles() {
        return qdrantService.listDocuments();
    }

    @GetMapping("/documents")
    public ResponseEntity<?> documents() {
        return ResponseEntity.ok(qdrantService.listDocuments());
    }
}