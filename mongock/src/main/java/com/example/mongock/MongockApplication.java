package com.example.mongock;


import com.example.mongock.service.ChangeUnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MongockApplication implements CommandLineRunner {

    @Value("${mongock.input.directory}")
    private String inputFileDirectory;

    @Value("${mongock.input.fileName}")
    private String inputFileName;


    private final ChangeUnitService changeUnitService;

    @Autowired
    public MongockApplication(ChangeUnitService changeUnitService) {
        this.changeUnitService = changeUnitService;
    }


	public static void main(String[] args) {
        SpringApplication.run(MongockApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Provide the path to your migrations JSON file
        changeUnitService.applyChangesFromJson("src/main/resources/migrations/"+ inputFileDirectory+ "/" + inputFileName);

    }
}

