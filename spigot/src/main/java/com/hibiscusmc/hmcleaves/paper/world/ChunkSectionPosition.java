package com.hibiscusmc.hmcleaves.paper.world;

import java.util.UUID;

public record ChunkSectionPosition(UUID worldId, int x, int y, int z) {

    public static ChunkSectionPosition at(UUID worldId, int x, int y, int z) {
        return new ChunkSectionPosition(worldId, x, y, z);
    }

    public ChunkPosition toChunkPosition() {
        return new ChunkPosition(this.worldId, this.x, this.z);
    }

}
