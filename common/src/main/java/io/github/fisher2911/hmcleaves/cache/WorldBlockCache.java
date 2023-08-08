/*
 *
 *  *     HMCLeaves
 *  *     Copyright (C) 2022  Hibiscus Creative Studios
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU General Public License
 *  *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package io.github.fisher2911.hmcleaves.cache;

import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.world.ChunkPosition;
import io.github.fisher2911.hmcleaves.world.Position;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class WorldBlockCache {

    private final UUID world;
    private final Map<ChunkPosition, ChunkBlockCache> blockCacheMap;

    public WorldBlockCache(UUID world, Map<ChunkPosition, ChunkBlockCache> blockCacheMap) {
        this.world = world;
        this.blockCacheMap = blockCacheMap;
    }

    @Nullable
    public ChunkBlockCache getChunkBlockCache(ChunkPosition chunkPosition) {
        return this.blockCacheMap.get(chunkPosition);
    }

    @Nullable
    public ChunkBlockCache getChunkBlockCache(Position position) {
        return this.getChunkBlockCache(position.getChunkPosition());
    }

    @Nullable
    public ChunkBlockCache getChunkBlockCache(int x, int z) {
        return this.getChunkBlockCache(ChunkPosition.at(this.world, x, z));
    }

    @Nullable
    public ChunkBlockCache removeChunkBlockCache(ChunkPosition chunkPosition) {
        return this.blockCacheMap.remove(chunkPosition);
    }

    @Nullable
    public ChunkBlockCache removeChunkBlockCache(Position position) {
        return this.removeChunkBlockCache(position.getChunkPosition());
    }

    public void addBlockData(Position position, BlockData blockData) {
        final ChunkBlockCache chunkBlockCache = Optional.ofNullable(this.getChunkBlockCache(position))
                .orElseGet(() -> this.createChunkBlockCache(position));
        chunkBlockCache.setBlockData(position, blockData);
    }

    private ChunkBlockCache createChunkBlockCache(Position position) {
        final ChunkBlockCache chunkBlockCache = new ChunkBlockCache(
                position.getChunkPosition(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>()
        );
        this.blockCacheMap.put(position.getChunkPosition(), chunkBlockCache);
        return chunkBlockCache;
    }

    @NotNull
    public BlockData getBlockData(Position position) {
        final ChunkBlockCache chunkBlockCache = this.getChunkBlockCache(position);
        if (chunkBlockCache == null) return BlockData.EMPTY;
        return chunkBlockCache.getBlockDataAt(position);
    }

    @NotNull
    public BlockData removeBlockData(Position position) {
        final ChunkBlockCache chunkBlockCache = this.getChunkBlockCache(position);
        if (chunkBlockCache == null) return BlockData.EMPTY;
        return chunkBlockCache.removeBlockDataAt(position);
    }

    @Unmodifiable
    public Map<ChunkPosition, ChunkBlockCache> getBlockCacheMap() {
        return Collections.unmodifiableMap(this.blockCacheMap);
    }

    public void addToDropPositions(Position position, BlockData blockData) {
        final ChunkBlockCache chunkBlockCache = this.getChunkBlockCache(position);
        if (chunkBlockCache == null) return;
        chunkBlockCache.addToDropPositions(position, blockData);
    }

    public @NotNull BlockData removeFromDropPositions(Position position) {
        final ChunkBlockCache chunkBlockCache = this.getChunkBlockCache(position);
        if (chunkBlockCache == null) return BlockData.EMPTY;
        return chunkBlockCache.removeFromDropPositions(position);
    }

    @NotNull
    public BlockData getDataAtDropPosition(Position position) {
        final ChunkBlockCache chunkBlockCache = this.getChunkBlockCache(position);
        if (chunkBlockCache == null) return BlockData.EMPTY;
        return chunkBlockCache.getDataAtDropPosition(position);
    }

    public void clearAll(Consumer<ChunkBlockCache> consumer) {
        this.blockCacheMap.entrySet().removeIf(entry -> {
            consumer.accept(entry.getValue());
            return true;
        });
    }

    public ChunkBlockCache addChunkCache(ChunkPosition chunkPosition) {
        final ChunkBlockCache chunkBlockCache = new ChunkBlockCache(
                chunkPosition,
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>()
        );
        final ChunkBlockCache previous = this.blockCacheMap.putIfAbsent(chunkPosition, chunkBlockCache);
        if (previous != null) return previous;
        return chunkBlockCache;
    }

}
