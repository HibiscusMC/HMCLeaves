package com.hibiscusmc.hmcleaves.common.world;

import com.hibiscusmc.hmcleaves.common.util.PositionUtils;

import java.util.UUID;

public record Position(UUID worldId, int x, int y, int z) {

    public static Position at(UUID worldId, int x, int y, int z) {
        return new Position(worldId, x, y, z);
    }

    public Position add(int x, int y, int z) {
        return new Position(this.worldId, this.x + x, this.y + y, this.z + z);
    }

    public Position sub(int x, int y, int z) {
        return this.add(-x, -y, -z);
    }

    public ChunkPosition toChunkPosition() {
        return new ChunkPosition(
                this.worldId,
                PositionUtils.coordToChunkCoord(this.x),
                PositionUtils.coordToChunkCoord(this.z)
        );
    }

    public ChunkSectionPosition toSectionPosition() {
        return new ChunkSectionPosition(
                this.worldId,
                PositionUtils.coordToChunkCoord(this.x),
                PositionUtils.coordToChunkCoord(this.y),
                PositionUtils.coordToChunkCoord(this.z)
        );
    }

}
