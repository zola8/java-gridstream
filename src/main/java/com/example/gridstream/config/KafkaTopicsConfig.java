package com.example.gridstream.config;


import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaTopicsConfig.class);

    public static final String GRID_POSITIONS_TOPIC = "grid.positions";

    @Bean
    public NewTopic gridPositionsTopic() {
        LOGGER.info("Declaring Kafka topic: {} (partitions=1, replication-factor=1)", GRID_POSITIONS_TOPIC);
        return new NewTopic(GRID_POSITIONS_TOPIC, 1, (short) 1);
    }

}
