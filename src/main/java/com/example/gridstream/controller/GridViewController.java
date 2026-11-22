package com.example.gridstream.controller;

import com.example.gridstream.service.SnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class GridViewController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GridViewController.class);

    private final SnapshotService snapshotService;

    public GridViewController(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @GetMapping("/stream")
    public SseEmitter stream() {
        LOGGER.info("New SSE client connecting to /stream");
        return snapshotService.register();
    }

}
