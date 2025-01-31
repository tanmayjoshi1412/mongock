package com.example.mongock.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FileMetadata {
    private String changeUnitId;
    private String fileName;

    // Getters and Setters
    public String getChangeUnitId() { return changeUnitId; }
    public void setChangeUnitId(String changeUnitId) { this.changeUnitId = changeUnitId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "changeUnitId='" + changeUnitId + '\'' +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}

