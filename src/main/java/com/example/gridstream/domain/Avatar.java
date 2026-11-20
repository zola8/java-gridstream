package com.example.gridstream.domain;

public class Avatar {

    private final String id;
    private int x;
    private int y;

    public Avatar(String id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void move(Direction direction, GridProperties grid) {
        this.x = Math.floorMod(this.x + direction.getDx(), grid.cols());
        this.y = Math.floorMod(this.y + direction.getDy(), grid.rows());
    }
}
