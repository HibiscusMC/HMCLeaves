package com.hibiscusmc.hmcleaves.paper.world;

import com.hibiscusmc.hmcleaves.paper.block.CustomBlock;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlockState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class LeavesChunk {

    private final ChunkPosition chunkPosition;
    private final Map<ChunkSectionPosition, LeavesChunkSection> sections;
    private final Function<ChunkSectionPosition, LeavesChunkSection> sectionCreator;
    private boolean dirty = false;

    public LeavesChunk(ChunkPosition chunkPosition, Map<ChunkSectionPosition, LeavesChunkSection> sections, Function<ChunkSectionPosition, LeavesChunkSection> sectionCreator) {
        this.chunkPosition = chunkPosition;
        this.sections = sections;
        this.sectionCreator = sectionCreator;
    }

    public ChunkPosition chunkPosition() {
        return this.chunkPosition;
    }

    public @UnmodifiableView Map<ChunkSectionPosition, LeavesChunkSection> getSections() {
        return Collections.unmodifiableMap(this.sections);
    }

    public @Nullable LeavesChunkSection getSection(ChunkSectionPosition position) {
        return this.sections.get(position);
    }

    public void editSection(ChunkSectionPosition position, Consumer<@Nullable LeavesChunkSection> consumer) {
        final LeavesChunkSection chunkSection = this.sections.get(position);
        consumer.accept(chunkSection);
    }

    public void editInsertSection(
            ChunkSectionPosition position,
            Consumer<LeavesChunkSection> consumer
    ) {
        final LeavesChunkSection chunkSection = this.sections.computeIfAbsent(position, this.sectionCreator);
        consumer.accept(chunkSection);
    }

    public @Nullable CustomBlockState getBlock(Position position) {
        final LeavesChunkSection section = this.sections.get(position.toSectionPosition());
        if (section == null) {
            return null;
        }
        return section.getBlock(position);
    }

    public void setBlock(Position position, CustomBlockState block) {
        this.editInsertSection(position.toSectionPosition(), section -> {
            section.setBlock(position, block);
            this.dirty = true;
        });
    }

    public @Nullable CustomBlockState removeBlock(Position position) {
        final LeavesChunkSection section = this.sections.get(position.toSectionPosition());
        if (section == null) {
            return null;
        }
        final CustomBlockState block = section.removeBlock(position);
        if (block != null) {
            this.dirty = true;
        }
        return block;
    }

    public boolean isEmpty() {
        return this.sections.isEmpty();
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void forEach(BiConsumer<Position, CustomBlockState> consumer) {
        for (LeavesChunkSection section : this.sections.values()) {
            section.forEach(consumer);
        }
    }

}
