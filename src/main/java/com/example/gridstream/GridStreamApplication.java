package com.example.gridstream;

import com.example.gridstream.config.KafkaTopicsConfig;
import com.example.gridstream.domain.GridProperties;
import com.example.gridstream.domain.SimulationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableConfigurationProperties({GridProperties.class, SimulationProperties.class})
@EnableScheduling
public class GridStreamApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(GridStreamApplication.class);

    private final GridProperties gridProperties;
    private final SimulationProperties simulationProperties;

    public GridStreamApplication(GridProperties gridProperties,
                                 SimulationProperties simulationProperties) {
        this.gridProperties = gridProperties;
        this.simulationProperties = simulationProperties;
    }

    public static void main(String[] args) {
        SpringApplication.run(GridStreamApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        LOGGER.info("GridStream Step 2 ready - avatar simulator running");
        LOGGER.info("Grid dimensions: {} rows x {} cols", gridProperties.rows(), gridProperties.cols());
        LOGGER.info("Simulation: {} avatars, tick every {} ms", simulationProperties.avatarCount(), simulationProperties.tickMs());
        LOGGER.info("Kafka topic: {}", KafkaTopicsConfig.GRID_POSITIONS_TOPIC);
        LOGGER.info("Verify: docker exec -it gridstream-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic grid.positions --from-beginning --property print.key=true");
        LOGGER.info("http://localhost:8080/actuator/health");
    }
}
