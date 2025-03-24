package com.hibiscusmc.hmcleaves.world;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public final class LeavesWorld {

    private final UUID worldId;
    private final Map<ChunkPosition, LeavesChunk> chunks;

    public LeavesWorld(UUID worldId, Map<ChunkPosition, LeavesChunk> chunks) {
        this.worldId = worldId;
        this.chunks = chunks;
    }

    public @UnmodifiableView Map<ChunkPosition, LeavesChunk> getChunks() {
        return Collections.unmodifiableMap(this.chunks);
    }

    public @Nullable LeavesChunk getChunk(ChunkPosition position) {
        return this.chunks.get(position);
    }

    public void editLeavesChunk(ChunkPosition position, Consumer<@Nullable LeavesChunk> consumer) {
        final LeavesChunk chunk = this.chunks.get(position);
        consumer.accept(chunk);
    }

    public void editInsertLeavesChunk(
            ChunkPosition position,
            Consumer<LeavesChunk> consumer,
            Function<ChunkPosition, LeavesChunk> function
    ) {
        final LeavesChunk chunk = this.chunks.computeIfAbsent(position, function);
        consumer.accept(chunk);
    }

    public UUID worldId() {
        return this.worldId;
    }

}
