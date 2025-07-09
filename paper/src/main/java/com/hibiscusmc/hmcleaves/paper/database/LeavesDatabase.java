package com.hibiscusmc.hmcleaves.paper.database;

import com.hibiscusmc.hmcleaves.paper.block.CustomBlock;
import com.hibiscusmc.hmcleaves.paper.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.paper.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.paper.world.Position;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public interface LeavesDatabase {

    Map<Position, CustomBlock> loadChunk(UUID worldId, ChunkPosition position) throws SQLException;

    void saveChunk(LeavesChunk chunk) throws SQLException;

    boolean hasChunkBeenScanned(UUID worldId, ChunkPosition position) throws SQLException;

    void setChunkScanned(UUID worldId, ChunkPosition position) throws SQLException;

    boolean init();

    void executeRead(Runnable runnable);

    void executeWrite(Runnable runnable);
}
