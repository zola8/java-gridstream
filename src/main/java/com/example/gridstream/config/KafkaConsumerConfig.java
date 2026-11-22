package com.example.gridstream.config;

import com.example.gridstream.domain.PositionUpdate;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public ConsumerFactory<String, PositionUpdate> consumerFactory(
        @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "gridstream-persist");       // factory default
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        LOGGER.info("Kafka consumer configured: bootstrapServers={}, groupId=gridstream-persist, valueDeserializer=JacksonJsonDeserializer (Jackson 3.x)",
            bootstrapServers);
        return new DefaultKafkaConsumerFactory<>(
            config,
            new StringDeserializer(),
            new JacksonJsonDeserializer<>(PositionUpdate.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PositionUpdate> kafkaListenerContainerFactory(
        ConsumerFactory<String, PositionUpdate> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, PositionUpdate> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
