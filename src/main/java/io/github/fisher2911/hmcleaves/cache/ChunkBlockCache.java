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
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

public class ChunkBlockCache {

    private final ChunkPosition chunkPosition;
    private final Map<Position, BlockData> blockDataMap;
    private final Map<Position, BlockData> removedPositions;
    private final Map<Position, BlockData> toDropPositions;
    private boolean dirty;
    private boolean saving;
    private boolean safeToMarkClean;

    public ChunkBlockCache(
            ChunkPosition chunkPosition,
            Map<Position, BlockData> blockDataMap,
            Map<Position, BlockData> removedPositions,
            Map<Position, BlockData> toDropPositions

    ) {
        this.chunkPosition = chunkPosition;
        this.blockDataMap = blockDataMap;
        this.removedPositions = removedPositions;
        this.toDropPositions = toDropPositions;
        this.dirty = false;
        this.saving = false;
        this.safeToMarkClean = true;
    }

    @NotNull
    public BlockData getBlockDataAt(Position position) {
        return this.blockDataMap.getOrDefault(position, BlockData.EMPTY);
    }

    public void setBlockData(Position position, BlockData blockData) {
        this.blockDataMap.put(position, blockData);
        this.removedPositions.remove(position);
        this.markDirty();
    }

    @NotNull
    public BlockData removeBlockDataAt(Position position) {
        final BlockData blockData = this.blockDataMap.remove(position);
        if (blockData == null) return BlockData.EMPTY;
        this.removedPositions.put(position, blockData);
        this.markDirty();
        return blockData;
    }

    public ChunkPosition getChunkPosition() {
        return this.chunkPosition;
    }

    @Unmodifiable
    public Map<Position, BlockData> getBlockDataMap() {
        return Collections.unmodifiableMap(this.blockDataMap);
    }

    public void addToDropPositions(Position position, BlockData blockData) {
        this.toDropPositions.put(position, blockData);
    }

    @NotNull
    public BlockData getDataAtDropPosition(Position position) {
        return this.toDropPositions.getOrDefault(position, BlockData.EMPTY);
    }

    public @NotNull BlockData removeFromDropPositions(Position position) {
        final BlockData data = this.toDropPositions.remove(position);
        if (data == null) return BlockData.EMPTY;
        return data;
    }

    public void clearToDropPositions() {
        this.toDropPositions.clear();
    }

    public void clearRemovedPositions(Function<Map.Entry<Position, BlockData>, Boolean> function) {
        this.removedPositions.entrySet().removeIf(function::apply);
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isClean() {
        return !this.dirty;
    }

    public void markDirty() {
        if (this.saving) {
            this.setSafeToMarkClean(false);
        }
        this.dirty = true;
    }

    public void markClean() {
        if (!this.isSafeToMarkClean()) return;
        this.dirty = false;
    }

    public boolean isSaving() {
        return this.saving;
    }

    public void setSaving(boolean saving) {
        this.saving = saving;
    }

    public boolean isSafeToMarkClean() {
        return this.safeToMarkClean;
    }

    public void setSafeToMarkClean(boolean safeToMarkClean) {
        if (this.saving) return;
        this.safeToMarkClean = safeToMarkClean;
    }

}
