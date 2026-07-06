package com.example.alchemy.dto;

public class FileInfo {

    private String documentId;
    private String fileName;
    private String sessionId;

    public FileInfo(String documentId, String fileName,String sessionId) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.sessionId=sessionId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getFileName() {
        return fileName;
    }
    public String sessionId(){
        return sessionId;
    }
}