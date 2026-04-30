package com.manifestreader.llmtask;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.manifestreader")
public class LlmTaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmTaskApplication.class, args);
    }
}
