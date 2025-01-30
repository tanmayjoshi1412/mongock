package com.example.mongock.Repository;

import com.example.mongock.model.ChangeLog;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface ChangeLogRepository extends MongoRepository<ChangeLog, String> {

    boolean existsByChangeUnitId(String changeUnitId);
}

