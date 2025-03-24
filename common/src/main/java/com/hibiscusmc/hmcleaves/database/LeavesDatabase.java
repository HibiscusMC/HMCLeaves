package com.hibiscusmc.hmcleaves.database;

import com.hibiscusmc.hmcleaves.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.world.LeavesChunk;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.UUID;

public interface LeavesDatabase {

    @Nullable
    LeavesChunk loadChunk(UUID worldId, ChunkPosition chunkPosition) throws SQLException;

    boolean init();
}
