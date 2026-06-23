package com.example.alchemy.Controller;

import com.example.alchemy.Service.FileProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public String upload(@RequestParam("files") MultipartFile[] files) throws Exception {

        if (files == null || files.length == 0) {
            return "No files uploaded";
        }

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                service.process(file);
            }
        }
        log.info("🚀 Upload API hit");


        return "Files processed successfully";
    }
}