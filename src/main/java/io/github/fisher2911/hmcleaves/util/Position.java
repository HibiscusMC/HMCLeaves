package io.github.fisher2911.hmcleaves.util;

import org.bukkit.Location;

import java.util.Objects;

public record Position(int x, int y, int z) {

    public static Position fromLocation(Location location) {
        return new Position(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public int getXInChunk() {
        return x & 15;
    }

    public int getZInChunk() {
        return z & 15;
    }

    @Override
    public String toString() {
        return "Position{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
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
