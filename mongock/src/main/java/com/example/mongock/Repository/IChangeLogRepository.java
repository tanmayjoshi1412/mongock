package com.example.mongock.Repository;

import com.example.mongock.model.ChangeLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;


public interface IChangeLogRepository extends MongoRepository<ChangeLog, String> {

    boolean existsByChangeUnitIdAndAppliedAtAfter(String changeUnitId, LocalDateTime appliedAt);

}

