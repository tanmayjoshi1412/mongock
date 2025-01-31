package com.example.mongock.model;

public enum OperationType {
    CREATE,   // Represents collection creation
    INSERT,   // Represents inserting documents
    UPDATE,   // Represents updating documents
    DELETE,   // Represents deleting documents
    RENAME,   // Represents renaming collection
    DROP;     // Represents dropping collection

    @Override
    public String toString() {
        return name().toLowerCase(); // Returns lowercase version (e.g., "create", "insert")
    }
}
