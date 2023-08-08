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

import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.cache.BlockCache;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Bukkit;

public class LeafDropUtil {

    private static final HMCLeaves PLUGIN = HMCLeaves.getPlugin(HMCLeaves.class);

    public static void addToDropPositions(BlockCache blockCache, Position position, BlockData blockData) {
        blockCache.addToDropPositions(position, blockData);
        Bukkit.getScheduler().runTaskLater(PLUGIN, () -> {
            final BlockData removed = blockCache.getDataAtDropPosition(position);
            if (removed != blockData) return;
            blockCache.removeFromDropPositions(position);
        }, 20);
    }

}
