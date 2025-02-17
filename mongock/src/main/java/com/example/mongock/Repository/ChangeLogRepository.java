package com.example.mongock.Repository;

import com.example.mongock.model.ChangeLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public class ChangeLogRepository {

    @Autowired
    private MongoTemplate mongoTemplate; // Ensure this is dynamically switched for the correct database

    public boolean existsByChangeUnitIdAndAppliedAtAfter(String changeUnitId, LocalDateTime appliedAfter) {
        Query query = new Query(Criteria.where("changeUnitId").is(changeUnitId).and("appliedAt").gt(appliedAfter));

        // Debugging logs
        System.out.println("Checking ChangeLog for ChangeUnit: " + changeUnitId);
        System.out.println("Using Database: " + mongoTemplate.getDb().getName());

        return mongoTemplate.exists(query, ChangeLog.class);
    }

    public void saveChangeLog(ChangeLog changeLog) {
        mongoTemplate.save(changeLog);
    }
}
