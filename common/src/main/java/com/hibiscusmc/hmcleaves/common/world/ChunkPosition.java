package com.hibiscusmc.hmcleaves.common.world;

import java.util.UUID;

public record ChunkPosition(UUID worldId, int x, int z) {

    public static ChunkPosition at(UUID worldId, int x, int z) {
        return new ChunkPosition(worldId, x, z);
    }

}
