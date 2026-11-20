package com.example.gridstream.domain;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "avatar_state")
public class AvatarState {

    @Id
    private String avatarId;
    private int x;
    private int y;
    private long tick;
    private Instant updatedAt;

    protected AvatarState() {
    }

    public AvatarState(String avatarId, int x, int y, long tick, Instant updatedAt) {
        this.avatarId = avatarId;
        this.x = x;
        this.y = y;
        this.tick = tick;
        this.updatedAt = updatedAt;
    }

    public String getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(String avatarId) {
        this.avatarId = avatarId;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public long getTick() {
        return tick;
    }

    public void setTick(long tick) {
        this.tick = tick;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
