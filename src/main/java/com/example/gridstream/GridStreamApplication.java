package com.example.gridstream;

import com.example.gridstream.config.KafkaTopicsConfig;
import com.example.gridstream.domain.GridProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.event.EventListener;


@SpringBootApplication
@ConfigurationPropertiesScan
public class GridStreamApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(GridStreamApplication.class);

    private final GridProperties gridProperties;

    public GridStreamApplication(GridProperties gridProperties) {
        this.gridProperties = gridProperties;
    }

    public static void main(String[] args) {
        SpringApplication.run(GridStreamApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        LOGGER.info("GridStream Step 1 ready - domain + topic declared");
        LOGGER.info("Grid dimensions: {} rows x {} cols", gridProperties.rows(), gridProperties.cols());
        LOGGER.info("Kafka topic: {}", KafkaTopicsConfig.GRID_POSITIONS_TOPIC);
        LOGGER.info("Verify: docker exec -it gridstream-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list");
        LOGGER.info("http://localhost:8080/actuator/health");
    }
}
