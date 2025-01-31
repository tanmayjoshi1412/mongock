package com.example.mongock.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionData {
    private String collectionName;
    private JsonNode insert;
    private JsonNode update;
    private JsonNode delete;

    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }

    public Optional<JsonNode> getInsert() { return Optional.ofNullable(insert); }
    public void setInsert(JsonNode insert) { this.insert = insert; }

    public Optional<JsonNode> getUpdate() { return Optional.ofNullable(update); }
    public void setUpdate(JsonNode update) { this.update = update; }

    public Optional<JsonNode> getDelete() { return Optional.ofNullable(delete); }
    public void setDelete(JsonNode delete) { this.delete = delete; }

    @Override
    public String toString() {
        return "CollectionData{" +
                "collectionName='" + collectionName + '\'' +
                ", insert=" + insert +
                ", update=" + update +
                ", delete=" + delete +
                '}';
    }
}
