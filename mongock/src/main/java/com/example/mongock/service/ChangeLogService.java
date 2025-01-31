package com.example.mongock.service;

import com.example.mongock.Repository.ChangeLogRepository;
import com.example.mongock.model.ChangeLog;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ChangeLogService {

    private final ChangeLogRepository changeLogRepository;

    public ChangeLogService(ChangeLogRepository changeLogRepository) {
        this.changeLogRepository = changeLogRepository;
    }

    public void logChangeLog(String changeUnitId, String operation, String collectionName, boolean isSuccess) {
        logChangeLog(changeUnitId, operation, Collections.singletonList(collectionName), isSuccess);
    }

    public void logChangeLog(String changeUnitId, String operation, List<String> collectionNames, boolean isSuccess) {
        ChangeLog changeLog = new ChangeLog();
        changeLog.setChangeUnitId(changeUnitId);
        changeLog.setOperation(operation);
        changeLog.setAppliedAt(LocalDateTime.now());
        changeLog.setSuccess(isSuccess);
        changeLog.setCollectionNames(collectionNames);
        changeLogRepository.save(changeLog);
        System.out.println("ChangeLog saved for changeUnit: " + changeUnitId);
    }
}
