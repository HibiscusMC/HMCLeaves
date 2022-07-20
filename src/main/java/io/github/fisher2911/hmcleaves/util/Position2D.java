package io.github.fisher2911.hmcleaves.util;

import java.util.Objects;
import java.util.UUID;

public record Position2D(UUID world, int x, int y) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Position2D that = (Position2D) o;
        return x == that.x && y == that.y && Objects.equals(world, that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y);
    }
}
