package com.example.gridstream.config;


import com.example.gridstream.domain.PositionUpdate;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProducerConfig.class);

    @Bean
    public ProducerFactory<String, PositionUpdate> producerFactory(
        @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        LOGGER.info("Kafka producer configured: bootstrapServers={}, keySerializer=StringSerializer, valueSerializer=JacksonJsonSerializer (Jackson 3.x)",
            bootstrapServers);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, PositionUpdate> kafkaTemplate(
        ProducerFactory<String, PositionUpdate> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

}
