package com.example.gridstream.consumer;

import com.example.gridstream.config.KafkaTopicsConfig;
import com.example.gridstream.domain.GridProperties;
import com.example.gridstream.domain.PositionUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AsciiRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsciiRenderer.class);

    private final GridProperties gridProperties;
    private final Map<String, PositionUpdate> currentPositions = new ConcurrentHashMap<>();

    public AsciiRenderer(GridProperties gridProperties) {
        this.gridProperties = gridProperties;
        LOGGER.info("AsciiRenderer initialized - consumer group=gridstream-renderer");
    }

    @KafkaListener(
        topics = KafkaTopicsConfig.GRID_POSITIONS_TOPIC,
        groupId = "gridstream-renderer"
    )
    public void consume(PositionUpdate event) {
        currentPositions.put(event.avatarId(), event);
    }

    @Scheduled(fixedDelayString = "${gridstream.simulation.tick-ms:2000}")
    public void render() {
        int rows = gridProperties.rows();
        int cols = gridProperties.cols();
        char[][] grid = new char[rows][cols];
        for (char[] row : grid) {
            Arrays.fill(row, '.');
        }
        for (PositionUpdate p : currentPositions.values()) {
            if (p.x() >= 0 && p.x() < cols && p.y() >= 0 && p.y() < rows) {
                char marker = p.avatarId().charAt(p.avatarId().length() - 1);
                grid[p.y()][p.x()] = marker;
            }
        }
        long latestTick = currentPositions.values().stream()
            .mapToLong(PositionUpdate::tick)
            .max()
            .orElse(0);

        StringBuilder sb = new StringBuilder("\n   ");
        for (int x = 0; x < cols; x++) {
            sb.append(x % 10);
        }
        sb.append("\n   +");
        sb.append("-".repeat(cols));
        sb.append("+");
        for (int y = 0; y < rows; y++) {
            sb.append(String.format("\n%2d|", y));
            for (int x = 0; x < cols; x++) {
                sb.append(grid[y][x]);
            }
            sb.append("|");
        }
        sb.append("\n   +");
        sb.append("-".repeat(cols));
        sb.append("+");
        LOGGER.info("ASCII grid (latest tick={}):\n{}", latestTick, sb);
    }
}
