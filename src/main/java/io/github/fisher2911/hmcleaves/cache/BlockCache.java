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

public class BlockCache {

    private final Map<UUID, WorldBlockCache> cache;

    public BlockCache(Map<UUID, WorldBlockCache> cache) {
        this.cache = cache;
    }

    @Unmodifiable
    public Map<UUID, WorldBlockCache> getCache() {
        return Collections.unmodifiableMap(this.cache);
    }

    @Nullable
    public WorldBlockCache getWorldBlockCache(UUID world) {
        return this.cache.get(world);
    }

    public void addBlockData(Position position, BlockData blockData) {
        final UUID world = position.world();
        Optional.ofNullable(this.getWorldBlockCache(world))
                .orElseGet(() -> this.addWorldBlockCache(world))
                .addBlockData(position, blockData);
    }

    private WorldBlockCache addWorldBlockCache(UUID world) {
        WorldBlockCache worldBlockCache = new WorldBlockCache(world, new ConcurrentHashMap<>());
        this.cache.put(world, worldBlockCache);
        return worldBlockCache;
    }

    @NotNull
    public BlockData getBlockData(Position position) {
        final WorldBlockCache worldBlockCache = this.getWorldBlockCache(position.world());
        if (worldBlockCache == null) return BlockData.EMPTY;
        return worldBlockCache.getBlockData(position);
    }

    @NotNull
    public BlockData removeBlockData(Position position) {
        final WorldBlockCache worldBlockCache = this.getWorldBlockCache(position.world());
        if (worldBlockCache == null) return BlockData.EMPTY;
        return worldBlockCache.removeBlockData(position);
    }

    @Nullable
    public ChunkBlockCache getChunkBlockCache(Position position) {
        return this.getChunkBlockCache(position.getChunkPosition());
    }

    @Nullable
    public ChunkBlockCache getChunkBlockCache(ChunkPosition position) {
        final WorldBlockCache worldBlockCache = this.getWorldBlockCache(position.world());
        if (worldBlockCache == null) return null;
        return worldBlockCache.getChunkBlockCache(position);
    }

    @Nullable
    public ChunkBlockCache removeChunkBlockCache(ChunkPosition position) {
        final WorldBlockCache worldBlockCache = this.getWorldBlockCache(position.world());
        if (worldBlockCache == null) return null;
        return worldBlockCache.removeChunkBlockCache(position);
    }
}
