package com.example.alchemy.dto;

public class FileInfo {

    private String documentId;
    private String fileName;

    public FileInfo(String documentId, String fileName) {
        this.documentId = documentId;
        this.fileName = fileName;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getFileName() {
        return fileName;
    }
}