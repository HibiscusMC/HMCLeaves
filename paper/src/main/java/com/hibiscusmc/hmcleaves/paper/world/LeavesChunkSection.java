package com.hibiscusmc.hmcleaves.paper.world;

import com.hibiscusmc.hmcleaves.paper.block.CustomBlockState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;

public final class LeavesChunkSection {

    private final ChunkSectionPosition chunkSectionPosition;
    private final Map<Position, CustomBlockState> blocks;

    public LeavesChunkSection(ChunkSectionPosition chunkSectionPosition, Map<Position, CustomBlockState> blocks) {
        this.chunkSectionPosition = chunkSectionPosition;
        this.blocks = blocks;
    }

    public ChunkSectionPosition sectionPosition() {
        return this.chunkSectionPosition;
    }

    public @UnmodifiableView Map<Position, CustomBlockState> getBlocks() {
        return Collections.unmodifiableMap(this.blocks);
    }

    public @Nullable CustomBlockState getBlock(Position position) {
        return this.blocks.get(position);
    }

    void setBlock(Position position, CustomBlockState block) {
        this.blocks.put(position, block);
    }

    @Nullable CustomBlockState removeBlock(Position position) {
        return this.blocks.remove(position);
    }

    public boolean isEmpty() {
        return this.blocks.isEmpty();
    }

    public void forEach(BiConsumer<Position, CustomBlockState> consumer) {
        this.blocks.forEach(consumer);
    }

}
