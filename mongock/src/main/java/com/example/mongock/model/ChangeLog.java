package com.example.mongock.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
@Document(collection = "changelog")
public class ChangeLog {

    @Id
    private String id;

    private String changeUnitId;
    private String operation;
    private List<String> collectionNames;
    private LocalDateTime appliedAt;
    private boolean success;
    private String  fileName;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChangeUnitId() {
        return changeUnitId;
    }

    public void setChangeUnitId(String changeUnitId) {
        this.changeUnitId = changeUnitId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public List<String> getCollectionNames() {
        return collectionNames;
    }

    public void setCollectionNames(List<String> collectionNames) {
        this.collectionNames = collectionNames;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
