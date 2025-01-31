package com.example.mongock.service;

import com.example.mongock.model.CollectionData;
import com.example.mongock.model.CreateData;
import com.example.mongock.model.OperationType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoNamespace;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mongodb.client.MongoDatabase;

import java.util.Collections;
import java.util.Map;

@Service
public class ChangeUnitService {

    @Autowired
    private ChangeLogService changeLogService;

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final MongoClient mongoClient; // For transactions

    @Autowired
    public ChangeUnitService(MongoTemplate mongoTemplate, ObjectMapper objectMapper, MongoClient mongoClient) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
        this.mongoClient = mongoClient;
    }

    public void createCollection(String changeUnitId, CreateData createData) {
        try {
            // Process the create operations
            processCreateOperations(changeUnitId, createData);

            // Log successful creation operation
            changeLogService.logChangeLog(changeUnitId, OperationType.CREATE.toString(), createData.getCreate(), true);
        } catch (Exception e) {
            // Log the exception in case of failure
            System.err.println("Error processing create operation for ChangeUnit: " + changeUnitId);
            e.printStackTrace();

            // Log failure
            changeLogService.logChangeLog(changeUnitId, OperationType.CREATE.toString(), createData.getCreate(), false);
        }
    }

    private void processCreateOperations(String changeUnitId, CreateData createData) {
        createData.getCreate().forEach(collectionName -> {
            if (!mongoTemplate.collectionExists(collectionName)) {
                mongoTemplate.createCollection(collectionName);
                System.out.println("Collection " + collectionName + " created.");
            } else {
                System.out.println("Collection " + collectionName + " already exists.");
            }
        });
    }

    @Transactional // Ensure each operation runs inside a transaction
    public void runOperationsOnCollection(String changeUnitId, CollectionData collectionData) {
        try (ClientSession session = mongoClient.startSession()) {
            session.startTransaction(); // Start a transaction

            processInsertOperation(changeUnitId, collectionData);
            processUpdateOperation(changeUnitId, collectionData);
            processDeleteOperation(changeUnitId, collectionData);

            session.commitTransaction(); // Commit transaction if everything succeeds
            System.out.println("Transaction committed for ChangeUnit: " + changeUnitId);
        } catch (Exception e) {
            System.err.println("Rolling back transaction for ChangeUnit: " + changeUnitId);
            e.printStackTrace();

            // Log failure in ChangeLog
            changeLogService.logChangeLog(changeUnitId, "TRANSACTION", Collections.emptyList(), false);
        }
    }

    private void processInsertOperation(String changeUnitId, CollectionData collectionData) {
        try {
            collectionData.getInsert().ifPresent(insertNode -> {
                String collectionName = collectionData.getCollectionName();

                if (!insertNode.isArray()) {
                    System.err.println("Insert operation skipped: Expected an array, but got " + insertNode);
                    return;
                }

                // Convert each JSON node into a Document and insert into MongoDB
                insertNode.forEach(document -> mongoTemplate.insert(Document.parse(document.toString()), collectionName));

                // Log the operation after successful insertion
                changeLogService.logChangeLog(changeUnitId, OperationType.INSERT.toString(), collectionName, true);
            });
        } catch (Exception e) {
            // Log the exception in case of failure
            System.err.println("Error processing insert operation for ChangeUnit: " + changeUnitId);
            e.printStackTrace();

            // Log failure
            changeLogService.logChangeLog(changeUnitId, OperationType.INSERT.toString(), collectionData.getCollectionName(), false);
        }
    }

    private void processUpdateOperation(String changeUnitId, CollectionData collectionData) {
        try {
            if (collectionData.getUpdate().isPresent()) {
                JsonNode updateNode = collectionData.getUpdate().get();
                JsonNode queriesNode = updateNode.get("queries");

                if (queriesNode == null || !queriesNode.isArray()) {
                    System.err.println("Update operation skipped: 'queries' field is missing or not an array.");
                    return;
                }

                // Processing each query in the "queries" array
                queriesNode.forEach(queryNode -> {
                    JsonNode query = queryNode.get("query");
                    JsonNode update = queryNode.get("update");

                    if (query != null && update != null) {
                        updateDocument(collectionData.getCollectionName(), query, update);
                    } else {
                        System.err.println("Skipping update operation due to missing 'query' or 'update'.");
                    }
                });
                // Log successful update operation
                changeLogService.logChangeLog(changeUnitId, OperationType.UPDATE.toString(), collectionData.getCollectionName(), true);
            }
        } catch (Exception e) {
            // Log the exception
            System.err.println("Error processing update operation for ChangeUnit: " + changeUnitId);
            e.printStackTrace();

            // Log failure
            changeLogService.logChangeLog(changeUnitId, OperationType.UPDATE.toString(), collectionData.getCollectionName(), false);
        }
    }

    private void updateDocument(String collectionName, JsonNode queryNode, JsonNode updateNode) {
        Map<String, Object> query = objectMapper.convertValue(queryNode, Map.class);
        Map<String, Object> update = objectMapper.convertValue(updateNode, Map.class);

        System.out.println("Executing update on: " + collectionName);
        System.out.println("Query: " + query);
        System.out.println("Update: " + update);

        // Ensure update is correctly formatted
        Document updateDocument;
        if (!update.containsKey("$set")) {
            updateDocument = new Document("$set", update);
        } else {
            updateDocument = new Document(update); // Already properly formatted
        }

        mongoTemplate.getCollection(collectionName).updateMany(
                new Document(query),
                updateDocument
        );

        System.out.println("Update completed.");
    }

    private void processDeleteOperation(String changeUnitId, CollectionData collectionData) {
        try {
            collectionData.getDelete().ifPresent(deleteNode -> {
                JsonNode queriesNode = deleteNode.get("queries");

                if (queriesNode == null || !queriesNode.isArray()) {
                    System.err.println("Delete operation skipped: 'queries' field is missing or not an array.");
                    return;
                }

                String collectionName = collectionData.getCollectionName();

                // Process each delete query
                queriesNode.forEach(queryNode -> {
                    JsonNode queryJsonNode = queryNode.get("query");

                    if (queryJsonNode == null) {
                        System.err.println("Skipping delete: Missing 'query' in " + queryNode);
                        return;
                    }

                    // Convert JsonNode to a valid MongoDB Document
                    Document queryDocument = objectMapper.convertValue(queryJsonNode, Document.class);

                    // Perform the deletion operation
                    mongoTemplate.getCollection(collectionName).deleteMany(queryDocument);
                    System.out.println("Deleted documents from collection: " + collectionName + " where query: " + queryDocument);
                });
                // Log successful delete operation
                changeLogService.logChangeLog(changeUnitId, OperationType.DELETE.toString(), collectionData.getCollectionName(), true);
            });
        } catch (Exception e) {
            // Log the exception in case of failure
            System.err.println("Error processing delete operation for ChangeUnit: " + changeUnitId);
            e.printStackTrace();

            // Log failure
            changeLogService.logChangeLog(changeUnitId, OperationType.DELETE.toString(), collectionData.getCollectionName(), false);
        }
    }

    // Method to handle dropping a collection
    public void dropCollection(String changeUnitId, String collectionName) {
        try {
            // Drop the collection
            mongoTemplate.getDb().getCollection(collectionName).drop();

            // Log the change
            changeLogService.logChangeLog(changeUnitId, OperationType.DROP.toString(), collectionName, true);
            System.out.println("Dropped collection: " + collectionName);
        } catch (Exception e) {
            System.err.println("Error dropping collection " + collectionName + ": " + e.getMessage());
            changeLogService.logChangeLog(changeUnitId, OperationType.DROP.toString(), collectionName, false);
        }
    }

    public void renameCollection(String changeUnitId, String oldCollectionName, String newCollectionName) {
        try {
            // Accessing MongoDatabase directly from MongoClient
            MongoDatabase database = mongoClient.getDatabase(mongoTemplate.getDb().getName());

            if (database != null) {
                // Check if the collection exists before renaming
                if (database.listCollectionNames().into(new java.util.ArrayList<>()).contains(oldCollectionName)) {
                    // Rename collection using MongoNamespace
                    database.getCollection(oldCollectionName).renameCollection(new MongoNamespace(database.getName(), newCollectionName));
                    System.out.println("Collection renamed from " + oldCollectionName + " to " + newCollectionName);

                    // Log the successful operation
                    changeLogService.logChangeLog(changeUnitId, OperationType.RENAME.toString(), oldCollectionName, true);
                } else {
                    System.out.println("Collection " + oldCollectionName + " does not exist.");
                    changeLogService.logChangeLog(changeUnitId, OperationType.RENAME.toString(), oldCollectionName, false);
                }
            } else {
                System.out.println("Failed to access MongoDatabase.");
            }
        } catch (Exception e) {
            System.err.println("Error processing rename operation for ChangeUnit: " + changeUnitId);
            e.printStackTrace();

            // Log failure in ChangeLog
            changeLogService.logChangeLog(changeUnitId, OperationType.RENAME.toString(), oldCollectionName, false);
        }
    }
}
