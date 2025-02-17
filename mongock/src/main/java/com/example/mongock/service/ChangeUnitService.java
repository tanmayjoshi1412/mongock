package com.example.mongock.service;

import com.example.mongock.model.CollectionData;
import com.example.mongock.model.CreateData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoNamespace;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ChangeUnitService {

    private final ChangeLogService changeLogService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ChangeUnitService(ChangeLogService changeLogService, ObjectMapper objectMapper) {
        this.changeLogService = changeLogService;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a collection if it does not exist.
     */
    public void createCollection(String changeUnitId, CreateData createData, MongoTemplate mongoTemplate) {
        try {
            for (String collectionName : createData.getCreate()) {
                if (!mongoTemplate.collectionExists(collectionName)) {
                    mongoTemplate.createCollection(collectionName);
                    System.out.println("Created collection: " + collectionName);
                } else {
                    System.out.println("Collection already exists: " + collectionName);
                }
            }
            changeLogService.logChangeLog(changeUnitId, "CREATE", createData.getCreate(), true,mongoTemplate);
        } catch (Exception e) {
            changeLogService.logChangeLog(changeUnitId, "CREATE", createData.getCreate(), false, mongoTemplate);
            e.printStackTrace();
        }
    }

    /**
     * Drops a collection if it exists.
     */
    public void dropCollection(String changeUnitId, String collectionName, MongoTemplate mongoTemplate) {
        try {
            if (mongoTemplate.collectionExists(collectionName)) {
                mongoTemplate.dropCollection(collectionName);
                System.out.println("Dropped collection: " + collectionName);
                changeLogService.logChangeLog(changeUnitId, "DROP", List.of(collectionName), true, mongoTemplate);
            } else {
                System.out.println("Collection does not exist: " + collectionName);
            }
        } catch (Exception e) {
            changeLogService.logChangeLog(changeUnitId, "DROP", List.of(collectionName), false, mongoTemplate);
            e.printStackTrace();
        }
    }

    /**
     * Renames a collection if it exists.
     */
    public void renameCollection(String changeUnitId, String oldCollectionName, String newCollectionName, MongoTemplate mongoTemplate) {
        try {
            if (mongoTemplate.collectionExists(oldCollectionName)) {
                mongoTemplate.getDb().getCollection(oldCollectionName)
                        .renameCollection(new MongoNamespace(mongoTemplate.getDb().getName(), newCollectionName));
                System.out.println("Renamed collection: " + oldCollectionName + " → " + newCollectionName);
                changeLogService.logChangeLog(changeUnitId, "RENAME", List.of(oldCollectionName, newCollectionName), true,mongoTemplate);
            } else {
                System.out.println("Collection not found for rename: " + oldCollectionName);
            }
        } catch (Exception e) {
            changeLogService.logChangeLog(changeUnitId, "RENAME", List.of(oldCollectionName, newCollectionName), false, mongoTemplate);
            e.printStackTrace();
        }
    }

    /**
     * Runs operations on a collection, including insert, update, and delete.
     */
    public void runOperationsOnCollection(String changeUnitId, CollectionData collectionData, MongoTemplate mongoTemplate) {
        try {
            processInsertOperation(changeUnitId, collectionData, mongoTemplate);
            processUpdateOperation(changeUnitId, collectionData, mongoTemplate);
            processDeleteOperation(changeUnitId, collectionData, mongoTemplate);
        } catch (Exception e) {
            changeLogService.logChangeLog(changeUnitId, "TRANSACTION", Collections.emptyList(), false, mongoTemplate);
            e.printStackTrace();
        }
    }

    /**
     * Inserts documents into a collection.
     */
    private void processInsertOperation(String changeUnitId, CollectionData collectionData, MongoTemplate mongoTemplate) {
        collectionData.getInsert().ifPresent(insertNode -> {  // Unwrap Optional<JsonNode>
            String collectionName = collectionData.getCollectionName();

            // Ensure insertNode is an array before iterating
            if (insertNode.isArray()) {
                for (JsonNode document : insertNode) {
                    mongoTemplate.insert(Document.parse(document.toString()), collectionName);
                }
                System.out.println("Inserted documents into collection: " + collectionName);
                changeLogService.logChangeLog(changeUnitId, "INSERT", collectionName, true, mongoTemplate);
            } else {
                System.err.println("Insert operation skipped: 'insert' field is not an array.");
            }
        });
    }



    /**
     * Updates documents in a collection.
     */
    private void processUpdateOperation(String changeUnitId, CollectionData collectionData, MongoTemplate mongoTemplate) {
        collectionData.getUpdate().ifPresent(updateNode -> {  // Unwrap Optional<JsonNode>
            String collectionName = collectionData.getCollectionName();
            JsonNode queriesNode = updateNode.get("queries");

            if (queriesNode != null && queriesNode.isArray()) {
                for (JsonNode queryNode : queriesNode) {
                    JsonNode query = queryNode.get("query");
                    JsonNode update = queryNode.get("update");
                    updateDocument(collectionName, query, update, mongoTemplate);
                }
                System.out.println("Updated documents in collection: " + collectionName);
                changeLogService.logChangeLog(changeUnitId, "UPDATE", collectionName, true,mongoTemplate);
            } else {
                System.err.println("Update operation skipped: 'queries' field is missing or not an array.");
            }
        });
    }


    private void updateDocument(String collectionName, JsonNode queryNode, JsonNode updateNode, MongoTemplate mongoTemplate) {
        Map<String, Object> query = objectMapper.convertValue(queryNode, Map.class);
        Map<String, Object> updateFields = objectMapper.convertValue(updateNode, Map.class);

        Document updateDocument;

        if (updateFields.containsKey("$set") || updateFields.containsKey("$inc") || updateFields.containsKey("$push")) {
            // ✅ Use the update fields directly if they already contain MongoDB update operators
            updateDocument = new Document(updateFields);
        } else {
            // ✅ Wrap in $set only if missing
            updateDocument = new Document("$set", updateFields);
        }

        mongoTemplate.getCollection(collectionName).updateMany(new Document(query), updateDocument);
    }


    /**
     * Deletes documents in a collection based on given queries.
     */
    private void processDeleteOperation(String changeUnitId, CollectionData collectionData, MongoTemplate mongoTemplate) {
        collectionData.getDelete().ifPresent(deleteNode -> {  // Unwrap Optional<JsonNode>
            String collectionName = collectionData.getCollectionName();
            JsonNode queriesNode = deleteNode.get("queries");

            if (queriesNode != null && queriesNode.isArray()) {
                for (JsonNode queryNode : queriesNode) {
                    JsonNode query = queryNode.get("query");
                    deleteDocument(collectionName, query, mongoTemplate);
                }
                System.out.println("Deleted documents in collection: " + collectionName);
                changeLogService.logChangeLog(changeUnitId, "DELETE", collectionName, true, mongoTemplate);
            } else {
                System.err.println("Delete operation skipped: 'queries' field is missing or not an array.");
            }
        });
    }



    private void deleteDocument(String collectionName, JsonNode queryNode, MongoTemplate mongoTemplate) {
        Map<String, Object> query = objectMapper.convertValue(queryNode, Map.class);
        mongoTemplate.getCollection(collectionName).deleteMany(new Document(query));
    }
}
