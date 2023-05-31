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

package io.github.fisher2911.hmcleaves.world;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.UUID;

public record ChunkPosition(UUID world, int x, int z) {

    public static ChunkPosition fromChunk(Chunk chunk) {
        return new ChunkPosition(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
    }

    public static ChunkPosition at(UUID world, int x, int z) {
        return new ChunkPosition(world, x, z);
    }

    public ChunkPosition add(int x, int z) {
        return new ChunkPosition(world, this.x + x, this.z + z);
    }

    public ChunkPosition add(ChunkPosition position) {
        return add(position.x, position.z);
    }

    public ChunkPosition subtract(int x, int z) {
        return new ChunkPosition(world, this.x - x, this.z - z);
    }

    public ChunkPosition subtract(ChunkPosition position) {
        return subtract(position.x, position.z);
    }

    public Chunk toChunk() throws IllegalStateException {
        final World world = Bukkit.getWorld(this.world);
        if (world == null) throw new IllegalStateException("World " + this.world + " is not loaded!");
        return world.getChunkAt(this.x, this.z);
    }

}
