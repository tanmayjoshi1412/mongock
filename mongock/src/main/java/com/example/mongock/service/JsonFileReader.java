package com.example.mongock.service;

import com.example.mongock.Repository.ChangeLogRepository;
import com.example.mongock.model.CollectionData;
import com.example.mongock.model.CreateData;
import com.example.mongock.model.FileMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class JsonFileReader {

    @Autowired
    private ChangeUnitService changeUnitService;

    private final ChangeLogRepository changeLogRepository;
    private final MongoTemplate mongoTemplate;
    private final MongoClient mongoClient; // For transactions

    @Value("${mongock.changeunit.file.list}")
    private String changeUnitMappingsFilePath;

    public JsonFileReader(ChangeLogRepository changeLogRepository, MongoTemplate mongoTemplate, MongoClient mongoClient) {
        this.changeLogRepository = changeLogRepository;
        this.mongoTemplate = mongoTemplate;
        this.mongoClient = mongoClient;
    }

    public void processChangeUnits() {
        ObjectMapper objectMapper = new ObjectMapper();

        try (ClientSession session = mongoClient.startSession()) {
            session.startTransaction(); // Start transaction

            List<FileMetadata> metadataList = objectMapper.readValue(
                    new File(changeUnitMappingsFilePath),
                    new TypeReference<List<FileMetadata>>() {}
            );

            // Sort execution order
            metadataList.sort(Comparator.comparing(FileMetadata::getChangeUnitId));

            for (FileMetadata metadata : metadataList) {
                String filePath = metadata.getFileName();
                System.out.println("\nProcessing file: " + filePath);
                JsonNode jsonTree = objectMapper.readTree(new File(filePath));

                if (isChangeUnitApplied(metadata.getChangeUnitId())) {
                    System.out.println("ChangeUnit (" + metadata.getChangeUnitId() + ") already applied.");
                    continue;
                }
                // Process drop operations
                if (jsonTree.has("drop")) {
                    for (JsonNode dropNode : jsonTree.get("drop")) {
                        String collectionName = dropNode.asText();
                        changeUnitService.dropCollection(metadata.getChangeUnitId(), collectionName);
                        System.out.println("Drop collection: " + collectionName);
                    }
                }else if (jsonTree.has("rename")) {
                    for (JsonNode renameNode : jsonTree.get("rename")) {
                        String oldCollectionName = renameNode.get("oldCollection").asText();
                        String newCollectionName = renameNode.get("newCollection").asText();
                        changeUnitService.renameCollection(metadata.getChangeUnitId(), oldCollectionName, newCollectionName);
                        System.out.println("Rename collection: " + oldCollectionName + " to " + newCollectionName);
                    }
                }else if (jsonTree.has("create")) {
                    CreateData createData = objectMapper.readValue(new File(filePath), CreateData.class);
                    changeUnitService.createCollection(metadata.getChangeUnitId(), createData);
                    System.out.println("Create Data: " + createData);
                } else {
                    CollectionData collectionData = objectMapper.readValue(new File(filePath), CollectionData.class);
                    changeUnitService.runOperationsOnCollection(metadata.getChangeUnitId(), collectionData);
                    System.out.println("Collection Data: " + collectionData);
                }
            }

            session.commitTransaction(); // Commit only if all steps succeed
            System.out.println("Transaction committed successfully.");

        } catch (Exception e) {
            System.err.println("Error encountered, rolling back transaction.");
            e.printStackTrace();
        }
    }

    private boolean isChangeUnitApplied(String changeUnitId) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return changeLogRepository.existsByChangeUnitIdAndAppliedAtAfter(changeUnitId, startOfDay);
    }
}

