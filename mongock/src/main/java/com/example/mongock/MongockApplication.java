package com.example.mongock;


import com.example.mongock.service.JsonFileReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MongockApplication implements CommandLineRunner {

    private final JsonFileReader jsonFileReader;

    @Autowired
    public MongockApplication(JsonFileReader jsonFileReader) {
        this.jsonFileReader = jsonFileReader;
    }


	public static void main(String[] args) {
        SpringApplication.run(MongockApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Provide the path to your migrations JSON file
        jsonFileReader.processChangeUnits();

    }
}

