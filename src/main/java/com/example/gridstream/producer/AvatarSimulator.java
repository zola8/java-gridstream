package com.example.gridstream.producer;

import com.example.gridstream.config.KafkaTopicsConfig;
import com.example.gridstream.domain.Avatar;
import com.example.gridstream.domain.Direction;
import com.example.gridstream.domain.GridProperties;
import com.example.gridstream.domain.PositionUpdate;
import com.example.gridstream.domain.SimulationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class AvatarSimulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AvatarSimulator.class);

    private final KafkaTemplate<String, PositionUpdate> kafkaTemplate;
    private final GridProperties gridProperties;
    private final List<Avatar> avatars;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private long tick = 0;

    public AvatarSimulator(KafkaTemplate<String, PositionUpdate> kafkaTemplate,
                           GridProperties gridProperties,
                           SimulationProperties simulationProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.gridProperties = gridProperties;

        LOGGER.info("Binding check: avatarCount={}, tickMs={}, gridRows={}, gridCols={}",
            simulationProperties.avatarCount(), simulationProperties.tickMs(),
            gridProperties.rows(), gridProperties.cols());

        this.avatars = new ArrayList<>(simulationProperties.avatarCount());

        for (int i = 0; i < simulationProperties.avatarCount(); i++) {
            int x = random.nextInt(gridProperties.cols());
            int y = random.nextInt(gridProperties.rows());
            avatars.add(new Avatar("avatar-" + (i + 1), x, y));
        }

        LOGGER.info("AvatarSimulator initialized: {} avatars on {}x{} grid, tick every {} ms",
            avatars.size(), gridProperties.cols(), gridProperties.rows(), simulationProperties.tickMs());
    }

    @Scheduled(fixedDelayString = "${gridstream.simulation.tick-ms:500}")
    public void tick() {
        tick++;
        for (Avatar avatar : avatars) {
            Direction direction = Direction.values()[random.nextInt(Direction.values().length)];
            avatar.move(direction, gridProperties);
            PositionUpdate event = new PositionUpdate(
                avatar.getId(),
                avatar.getX(),
                avatar.getY(),
                tick
            );
            kafkaTemplate.send(KafkaTopicsConfig.GRID_POSITIONS_TOPIC, avatar.getId(), event);
        }
        if (tick % 10 == 0) {
            LOGGER.debug("Tick {} - published {} position updates", tick, avatars.size());
        }
    }

}
