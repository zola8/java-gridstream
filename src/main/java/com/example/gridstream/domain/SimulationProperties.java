package com.example.gridstream.domain;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gridstream.simulation")
public record SimulationProperties(int avatarCount, long tickMs) {
}
