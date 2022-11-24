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

package io.github.fisher2911.hmcleaves.util;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

public class ChunkUtil {

    public static long chunkKeyAt(Location location) {
        return chunkKeyAt(location.getBlockX(), location.getBlockZ());
    }

    public static long chunkKeyAt(int x, int z) {
        return (long) x & 0xffffffffL | ((long) z & 0xffffffffL) << 32;
    }


    public static int getCoordInChunk(int i) {
        return i & 15;
    }

    @Nullable
    public static ChunkSnapshot getSnapshotAt(Location location) {
        final int chunkX = location.getBlockX() >> 4;
        final int chunkZ = location.getBlockZ() >> 4;
        final World world = location.getWorld();
        if (world == null || !world.isChunkLoaded(chunkX, chunkZ)) return null;
        final long chunkKey = ChunkUtil.chunkKeyAt(chunkX, chunkZ);
        return world.getChunkAt(chunkX, chunkZ).getChunkSnapshot();
    }

}
