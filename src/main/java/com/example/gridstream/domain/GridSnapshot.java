package com.example.gridstream.domain;

import java.util.List;

public record GridSnapshot(long tick, List<AvatarPos> avatars, int rows, int cols) {
}
