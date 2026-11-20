package com.example.gridstream.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gridstream.grid")
public record GridProperties(int rows, int cols) {
}
