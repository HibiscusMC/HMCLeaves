package com.hibiscusmc.hmcleaves.common.database;

import com.hibiscusmc.hmcleaves.common.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.common.world.ChunkSectionPosition;
import com.hibiscusmc.hmcleaves.common.world.LeavesChunk;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public interface LeavesDatabase {

    @Nullable
    LeavesChunk loadChunk(UUID worldId, ChunkPosition position) throws SQLException;

    void saveChunk(LeavesChunk chunk) throws SQLException;

    boolean hasChunkBeenScanned(UUID worldId, ChunkPosition position) throws SQLException;

    void setChunkScanned(UUID worldId, ChunkPosition position) throws SQLException;

    boolean init();

    void executeRead(Runnable runnable);

    void executeWrite(Runnable runnable);
}
