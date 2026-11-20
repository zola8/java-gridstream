package com.example.gridstream.consumer;

import com.example.gridstream.config.KafkaTopicsConfig;
import com.example.gridstream.domain.AvatarState;
import com.example.gridstream.domain.AvatarStateRepository;
import com.example.gridstream.domain.PositionHistoryEntry;
import com.example.gridstream.domain.PositionHistoryRepository;
import com.example.gridstream.domain.PositionUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PositionPersistConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PositionPersistConsumer.class);

    private final AvatarStateRepository avatarStateRepository;
    private final PositionHistoryRepository positionHistoryRepository;
    private final AtomicLong consumedCount = new AtomicLong(0);

    public PositionPersistConsumer(AvatarStateRepository avatarStateRepository,
                                   PositionHistoryRepository positionHistoryRepository) {
        this.avatarStateRepository = avatarStateRepository;
        this.positionHistoryRepository = positionHistoryRepository;
        LOGGER.info("PositionPersistConsumer initialized - persisting events to avatar_state and position_history");
    }

    @KafkaListener(topics = KafkaTopicsConfig.GRID_POSITIONS_TOPIC)
    @Transactional
    public void consume(PositionUpdate event) {
        Instant now = Instant.now();
        AvatarState state = new AvatarState(event.avatarId(), event.x(), event.y(), event.tick(), now);
        avatarStateRepository.save(state);
        PositionHistoryEntry entry = new PositionHistoryEntry(event.avatarId(), event.x(), event.y(), event.tick(), now);
        positionHistoryRepository.save(entry);

        long count = consumedCount.incrementAndGet();
        if (count % 30 == 0) {
            LOGGER.debug("Persisted {} events - latest: avatar={} tick={}", count, event.avatarId(), event.tick());
        }
    }

}
