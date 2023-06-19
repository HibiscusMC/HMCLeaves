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

import io.github.fisher2911.hmcleaves.cache.BlockCache;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.CaveVinesPlant;

public class ChainedBlockDestroyUtil {

    public static void handleBlockBreak(Block broken, BlockCache blockCache, LeavesConfig leavesConfig) {
        if (Tag.CAVE_VINES.isTagged(broken.getType()) || Tag.CAVE_VINES.isTagged(broken.getRelative(BlockFace.DOWN).getType())) {
//            handleCaveVinesDestroy(broken, blockCache, leavesConfig);
            return;
        }
    }

    private static void handleCaveVinesDestroy(Block broken, BlockCache blockCache, LeavesConfig leavesConfig) {
        Block below = broken.getRelative(BlockFace.DOWN);
        while (Tag.CAVE_VINES.isTagged(below.getType())) {
            blockCache.removeBlockData(Position.fromLocation(below.getLocation()));
            below = below.getRelative(BlockFace.DOWN);
        }
        final Block above = broken.getRelative(BlockFace.UP);
        if (Tag.CAVE_VINES.isTagged(above.getType())) {
            blockCache.addBlockData(
                    Position.fromLocation(above.getLocation()),
                    leavesConfig.getDefaultCaveVinesData(((CaveVinesPlant) above.getBlockData()).isBerries())
            );
        }
    }

}
