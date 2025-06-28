package de.mw.spring.example;

import de.mw.spring.asyncjpastreaming.AsyncJPAStreaming;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackageClasses = {ExampleApplication.class, AsyncJPAStreaming.class})
@EnableAsync
class ExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }
}