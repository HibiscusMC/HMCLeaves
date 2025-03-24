package com.hibiscusmc.hmcleaves.world;

import com.hibiscusmc.hmcleaves.block.LeavesBlock;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.Map;

public final class LeavesChunk {

    private final ChunkPosition chunkPosition;
    private final Map<Position, LeavesBlock> blocks;

    public LeavesChunk(ChunkPosition chunkPosition, Map<Position, LeavesBlock> blocks) {
        this.chunkPosition = chunkPosition;
        this.blocks = blocks;
    }

    public ChunkPosition chunkPosition() {
        return this.chunkPosition;
    }

    public @UnmodifiableView Map<Position, LeavesBlock> getBlocks() {
        return Collections.unmodifiableMap(this.blocks);
    }

    public @Nullable LeavesBlock getBlock(Position position) {
        return this.blocks.get(position);
    }

    public void setBlock(Position position, LeavesBlock block) {
        this.blocks.put(position, block);
    }

    public @Nullable LeavesBlock removeBlock(Position position) {
        return this.blocks.remove(position);
    }

    public boolean isEmpty() {
        return this.blocks.isEmpty();
    }

}
