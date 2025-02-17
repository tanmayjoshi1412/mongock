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

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class JsonFileReader {

    @Autowired
    private ChangeUnitService changeUnitService;

    private final ChangeLogRepository changeLogRepository;
    private final MongoClient mongoClient;
    private final ObjectMapper objectMapper;
    private final Map<String, MongoTemplate> mongoTemplates = new HashMap<>();

    @Value("${mongock.changeunit.file.list}")
    private String changeUnitMappingsFilePath;

    @Autowired
    public JsonFileReader(ChangeLogRepository changeLogRepository, MongoClient mongoClient, ObjectMapper objectMapper) {
        this.changeLogRepository = changeLogRepository;
        this.mongoClient = mongoClient;
        this.objectMapper = objectMapper;
    }

    public void processChangeUnits() {
        try (ClientSession session = mongoClient.startSession()) {
            session.startTransaction();

            JsonNode rootNode = objectMapper.readTree(new File(changeUnitMappingsFilePath));

            for (Iterator<String> it = rootNode.fieldNames(); it.hasNext(); ) {
                String databaseName = it.next();
                System.out.println("\nProcessing database: " + databaseName);

                JsonNode databaseChangeUnits = rootNode.get(databaseName);
                processDatabase(databaseName, databaseChangeUnits);
            }

            session.commitTransaction();
            System.out.println("All databases processed successfully.");

        } catch (Exception e) {
            System.err.println("Error encountered, rolling back transaction.");
            e.printStackTrace();
        }
    }

    private void processDatabase(String databaseName, JsonNode changeUnits) throws IOException {
        MongoTemplate mongoTemplate = getMongoTemplateForDatabase(databaseName);

        List<FileMetadata> metadataList = objectMapper.readValue(changeUnits.toString(), new TypeReference<List<FileMetadata>>() {});
        metadataList.sort(Comparator.comparing(FileMetadata::getChangeUnitId));

        for (FileMetadata metadata : metadataList) {
            if (isChangeUnitApplied(metadata.getChangeUnitId(), mongoTemplate)) {
                System.out.println("ChangeUnit (" + metadata.getChangeUnitId() + ") already applied.");
                continue;
            }

            String filePath = metadata.getFileName();
            JsonNode jsonTree = objectMapper.readTree(new File(filePath));

            // Handle different operations (drop, rename, create, etc.)
            if (jsonTree.has("drop")) {
                for (JsonNode dropNode : jsonTree.get("drop")) {
                    changeUnitService.dropCollection(metadata.getChangeUnitId(), dropNode.asText(), mongoTemplate);
                }
            } else if (jsonTree.has("rename")) {
                for (JsonNode renameNode : jsonTree.get("rename")) {
                    changeUnitService.renameCollection(metadata.getChangeUnitId(),
                            renameNode.get("oldCollection").asText(),
                            renameNode.get("newCollection").asText(),
                            mongoTemplate);
                }
            } else if (jsonTree.has("create")) {
                CreateData createData = objectMapper.readValue(new File(filePath), CreateData.class);
                changeUnitService.createCollection(metadata.getChangeUnitId(), createData, mongoTemplate);
            } else {
                CollectionData collectionData = objectMapper.readValue(new File(filePath), CollectionData.class);
                changeUnitService.runOperationsOnCollection(metadata.getChangeUnitId(), collectionData, mongoTemplate);
            }
        }
    }

    private boolean isChangeUnitApplied(String changeUnitId, MongoTemplate mongoTemplate) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();

        boolean collectionExists = mongoTemplate.getDb().listCollectionNames()
                .into(new ArrayList<>())
                .contains("changelog");

        if (!collectionExists) {
            System.out.println("ChangeLog collection does not exist in database: " + mongoTemplate.getDb().getName());
            return false;
        }

        return changeLogRepository.existsByChangeUnitIdAndAppliedAtAfter(changeUnitId, startOfDay);
    }

    private MongoTemplate getMongoTemplateForDatabase(String databaseName) {
        return mongoTemplates.computeIfAbsent(databaseName, dbName -> {
            System.out.println("Creating MongoTemplate for database: " + dbName);
            return new MongoTemplate(mongoClient, dbName);
        });
    }
}
