package com.example.mongock.service;

import com.example.mongock.Repository.ChangeLogRepository;
import com.example.mongock.model.ChangeLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ChangeLogService {

    private final ChangeLogRepository changeLogRepository;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public ChangeLogService(ChangeLogRepository changeLogRepository, MongoTemplate mongoTemplate) {
        this.changeLogRepository = changeLogRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public void logChangeLog(String changeUnitId, String operation, String collectionName, boolean isSuccess, MongoTemplate mongoTemplate) {
        logChangeLog(changeUnitId, operation, Collections.singletonList(collectionName), isSuccess, mongoTemplate);
    }

    public void logChangeLog(String changeUnitId, String operation, List<String> collectionNames, boolean isSuccess, MongoTemplate mongoTemplate) {
        try {
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChangeUnitId(changeUnitId);
            changeLog.setOperation(operation);
            changeLog.setAppliedAt(LocalDateTime.now());
            changeLog.setSuccess(isSuccess);
            changeLog.setCollectionNames(collectionNames);

            mongoTemplate.save(changeLog, "change_log");  // Ensure correct database
            System.out.println("ChangeLog saved in database: " + mongoTemplate.getDb().getName());
        } catch (Exception e) {
            System.err.println("Error saving change log: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
