package com.hibiscusmc.hmcleaves.world;

import java.util.UUID;

public record ChunkPosition(UUID worldId, int x, int y, int z) {

    public static ChunkPosition at(UUID worldId, int x, int y, int z) {
        return new ChunkPosition(worldId, x, y, z);
    }

}
