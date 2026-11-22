package com.example.gridstream.service;

import com.example.gridstream.domain.GridSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SnapshotService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotService.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            LOGGER.info("SSE client disconnected. Active clients: {}", emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            LOGGER.warn("SSE client timed out. Active clients: {}", emitters.size());
        });
        emitter.onError(e -> {
            emitters.remove(emitter);
            LOGGER.warn("SSE client error: {}. Active clients: {}", e.getMessage(), emitters.size());
        });
        LOGGER.info("SSE client registered. Active clients: {}", emitters.size());
        return emitter;
    }

    public void push(GridSnapshot snapshot) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("snapshot")
                    .data(snapshot, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                emitter.completeWithError(e);
                emitters.remove(emitter);
            }
        }
    }

}
