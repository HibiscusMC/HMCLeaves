package io.github.fisher2911.hmcleaves.util;

import java.util.Objects;

public record Position(int x, int y, int z) {

    public int getXInChunk() {
        return x & 15;
    }

    public int getZInChunk() {
        return z & 15;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Position position = (Position) o;
        return x == position.x && y == position.y && z == position.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
