package com.example.mongock.service;

import com.example.mongock.Repository.ChangeLogRepository;
import com.example.mongock.model.ChangeLog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChangeUnitService {

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final ChangeLogRepository changeLogRepository;

    @Autowired
    public ChangeUnitService(MongoTemplate mongoTemplate, ObjectMapper objectMapper, ChangeLogRepository changeLogRepository) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
        this.changeLogRepository = changeLogRepository;
    }

    // Apply changes from JSON file (reads and processes the operations)
    public void applyChangesFromJson(String jsonFilePath) throws IOException {
        JsonNode changeUnitsNode = objectMapper.readTree(new File(jsonFilePath));

        Iterator<JsonNode> elements = changeUnitsNode.elements();
        while (elements.hasNext()) {
            JsonNode changeUnit = elements.next();
            String changeUnitId = changeUnit.get("changeUnitId").asText();

            // If this ChangeUnit has been applied already, skip it.
            if (isChangeUnitApplied(changeUnitId)) {
                System.out.println("ChangeUnit (" + changeUnitId + ") already applied.");
                continue;
            }

            // Process 'create' operation separately
            processCreateOperations(changeUnit);

            // Process 'insert' and 'update' operations
            processInsertOperation(changeUnit);
            processUpdateOperation(changeUnit);

            // Log the applied change
            logChangeLog(changeUnitId, "batchOperation", "Processed batch operations");
        }
    }

    // Check if a ChangeUnit has already been applied
    private boolean isChangeUnitApplied(String changeUnitId) {
        return changeLogRepository.existsByChangeUnitId(changeUnitId);
    }

    // Process 'create' operation (create collections if not exist)
    private void processCreateOperations(JsonNode changeUnit) {
        JsonNode createNode = changeUnit.get("create");
        if (createNode != null) {
            Iterator<JsonNode> createCollectionNames = createNode.elements();
            while (createCollectionNames.hasNext()) {
                String collectionName = createCollectionNames.next().asText();
                // Logic to create collection if it does not exist
                if (!mongoTemplate.collectionExists(collectionName)) {
                    mongoTemplate.createCollection(collectionName);
                    System.out.println("Collection " + collectionName + " created.");
                    logChangeLog(changeUnit.get("changeUnitId").asText(), "create", collectionName);
                } else {
                    System.out.println("Collection " + collectionName + " already exists.");
                }
            }
        }
    }

    // Process 'insert' operation
    private void processInsertOperation(JsonNode changeUnit) {
        JsonNode insertNode = changeUnit.get("insert");
        if (insertNode != null) {
            String collectionName = changeUnit.get("collectionName").asText();
            if (insertNode.isArray()) {
                insertNode.forEach(document -> {
                    Document doc = Document.parse(document.toString());
                    mongoTemplate.insert(doc, collectionName);
                });
                logChangeLog(changeUnit.get("changeUnitId").asText(), "insert", collectionName);
            }
        }
    }

    // Process 'update' operation
    private void processUpdateOperation(JsonNode changeUnit) {
        JsonNode updateNode = changeUnit.get("update");
        if (updateNode != null) {
            JsonNode queriesNode = updateNode.get("queries");
            queriesNode.forEach(queryNode -> {
                JsonNode query = queryNode.get("query");
                JsonNode update = queryNode.get("update");
                if (query != null && update != null) {
                    String collectionName = changeUnit.get("collectionName").asText();
                    updateDocument(collectionName, query, update);
                }
            });
        }
    }

    // Update the document based on query and update nodes
    private void updateDocument(String collectionName, JsonNode queryNode, JsonNode updateNode) {
        Map<String, Object> query = objectMapper.convertValue(queryNode, Map.class);
        Map<String, Object> update = objectMapper.convertValue(updateNode, Map.class);

        mongoTemplate.getCollection(collectionName).updateOne(
                new org.bson.Document(query),
                new org.bson.Document(update)
        );
        System.out.println("Updated document in collection: " + collectionName);
    }

    // Log the change for this ChangeUnit and its collection operations
    private void logChangeLog(String changeUnitId, String operation, String collectionName) {
        ChangeLog changeLog = new ChangeLog();
        changeLog.setChangeUnitId(changeUnitId);
        changeLog.setOperation(operation);
        changeLog.setAppliedAt(LocalDateTime.now());
        changeLog.setSuccess(true);

        List<String> collectionNames = Collections.singletonList(collectionName);
        changeLog.setCollectionNames(collectionNames);

        changeLogRepository.save(changeLog);
        System.out.println("ChangeLog saved for changeUnit: " + changeUnitId);
    }
}
