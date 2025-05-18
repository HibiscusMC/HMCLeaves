package com.hibiscusmc.hmcleaves.common.world;

import com.hibiscusmc.hmcleaves.common.block.LeavesBlock;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.Map;

public final class LeavesChunkSection {

    private final ChunkSectionPosition chunkSectionPosition;
    private final Map<Position, LeavesBlock> blocks;

    public LeavesChunkSection(ChunkSectionPosition chunkSectionPosition, Map<Position, LeavesBlock> blocks) {
        this.chunkSectionPosition = chunkSectionPosition;
        this.blocks = blocks;
    }

    public ChunkSectionPosition sectionPosition() {
        return this.chunkSectionPosition;
    }

    public @UnmodifiableView Map<Position, LeavesBlock> getBlocks() {
        return Collections.unmodifiableMap(this.blocks);
    }

    public @Nullable LeavesBlock getBlock(Position position) {
        return this.blocks.get(position);
    }

    void setBlock(Position position, LeavesBlock block) {
        this.blocks.put(position, block);
    }

    @Nullable LeavesBlock removeBlock(Position position) {
        return this.blocks.remove(position);
    }

    public boolean isEmpty() {
        return this.blocks.isEmpty();
    }

}
