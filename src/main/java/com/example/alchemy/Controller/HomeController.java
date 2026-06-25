package com.example.alchemy.Controller;

import com.example.alchemy.Service.QdrantService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private final QdrantService qdrantService;

    public HomeController(QdrantService qdrantService) {
        this.qdrantService = qdrantService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/api/files")
    @ResponseBody
    public List<Map<String, String>> listFiles() {
        return qdrantService.listDocuments();
    }

    @DeleteMapping("/api/files/{documentId}")
    @ResponseBody
    public ResponseEntity<String> deleteFile(@PathVariable String documentId) {
        try {
            qdrantService.deleteDocument(documentId);
            return ResponseEntity.ok("Deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting file: " + e.getMessage());
        }
    }
}
