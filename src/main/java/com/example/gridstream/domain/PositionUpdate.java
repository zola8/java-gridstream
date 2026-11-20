package com.example.gridstream.domain;


public record PositionUpdate(
    String avatarId,
    int x,
    int y,
    long tick
) {
}
