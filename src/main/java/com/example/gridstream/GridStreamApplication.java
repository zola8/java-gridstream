package com.example.gridstream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class GridStreamApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(GridStreamApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(GridStreamApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        LOGGER.info("GridStream skeleton ready - Step 0 complete");
        LOGGER.info("Verify: open http://localhost:8080/actuator/health");
    }
}
