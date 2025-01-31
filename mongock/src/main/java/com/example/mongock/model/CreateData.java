package com.example.mongock.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateData {
    private List<String> create;

    public List<String> getCreate() { return create; }
    public void setCreate(List<String> create) { this.create = create; }

    @Override
    public String toString() {
        return "CreateData{" +
                "create=" + create +
                '}';
    }
}

