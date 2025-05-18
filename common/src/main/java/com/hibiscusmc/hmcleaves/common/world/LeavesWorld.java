package com.hibiscusmc.hmcleaves.common.world;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public final class LeavesWorld {

    private final UUID worldId;
    private final Map<ChunkPosition, LeavesChunk> chunks;
    private final Function<ChunkPosition, LeavesChunk> chunkCreator;
    private final Set<ChunkPosition> loadingChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public LeavesWorld(UUID worldId, Map<ChunkPosition, LeavesChunk> chunks, Function<ChunkPosition, LeavesChunk> chunkCreator) {
        this.worldId = worldId;
        this.chunks = chunks;
        this.chunkCreator = chunkCreator;
    }

    public @UnmodifiableView Map<ChunkPosition, LeavesChunk> getChunks() {
        return Collections.unmodifiableMap(this.chunks);
    }

    public @Nullable LeavesChunk getChunk(ChunkPosition position) {
        return this.chunks.get(position);
    }

    public void editChunk(ChunkPosition position, Consumer<@Nullable LeavesChunk> consumer) {
        final LeavesChunk chunk = this.chunks.get(position);
        consumer.accept(chunk);
    }

    public void editInsertChunk(
            ChunkPosition position,
            Consumer<LeavesChunk> consumer
    ) {
        final LeavesChunk chunk = this.chunks.computeIfAbsent(position, chunkCreator);
        consumer.accept(chunk);
    }

    public void setChunk(LeavesChunk chunk) {
        this.chunks.put(chunk.chunkPosition(), chunk);
    }

    public void removeChunk(ChunkPosition position) {
        this.chunks.remove(position);
    }

    public void setLoadingChunk(ChunkPosition position) {
        this.loadingChunks.add(position);
    }

    public void removeLoadingChunk(ChunkPosition position) {
        this.loadingChunks.remove(position);
    }

    public boolean isLoadingChunk(ChunkPosition position) {
        return this.loadingChunks.contains(position);
    }

    public UUID worldId() {
        return this.worldId;
    }

}
