package com.smartdesk.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SmartDeskSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartDeskSystemApplication.class, args);
    }

}
