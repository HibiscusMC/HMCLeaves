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
import io.github.fisher2911.hmcleaves.data.AgeableData;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.data.CaveVineData;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class ChainedBlockUtil {

    public static void handleBlockBreak(Block broken, BlockCache blockCache, LeavesConfig leavesConfig) {
        final Block below = broken.getRelative(BlockFace.DOWN);
        final Block above = broken.getRelative(BlockFace.UP);
        final Material brokenMaterial = broken.getType();
        final Material belowMaterial = below.getType();
        final Material aboveMaterial = above.getType();
        if (Tag.CAVE_VINES.isTagged(broken.getType()) || Tag.CAVE_VINES.isTagged(broken.getRelative(BlockFace.DOWN).getType())) {
            handleBlockDestroy(
                    broken,
                    blockCache,
                    BlockFace.UP,
                    Tag.CAVE_VINES::isTagged,
                    leavesConfig
            );
            return;
        }
        if (brokenMaterial == Material.SUGAR_CANE || aboveMaterial == Material.SUGAR_CANE) {
            handleBlockDestroy(
                    broken,
                    blockCache,
                    BlockFace.DOWN,
                    m -> m == Material.SUGAR_CANE,
                    leavesConfig
            );
            return;
        }
        if (brokenMaterial == Material.KELP || brokenMaterial == Material.KELP_PLANT ||
                aboveMaterial == Material.KELP || aboveMaterial == Material.KELP_PLANT) {
            handleBlockDestroy(
                    broken,
                    blockCache,
                    BlockFace.DOWN,
                    m -> m == Material.KELP || m == Material.KELP_PLANT,
                    leavesConfig
            );
            return;
        }
        if (brokenMaterial == Material.WEEPING_VINES || brokenMaterial == Material.WEEPING_VINES_PLANT ||
                belowMaterial == Material.WEEPING_VINES || belowMaterial == Material.WEEPING_VINES_PLANT) {
            handleBlockDestroy(
                    broken,
                    blockCache,
                    BlockFace.UP,
                    m -> m == Material.WEEPING_VINES || m == Material.WEEPING_VINES_PLANT,
                    leavesConfig
            );
            return;
        }
        if (brokenMaterial == Material.TWISTING_VINES || brokenMaterial == Material.TWISTING_VINES_PLANT ||
                aboveMaterial == Material.TWISTING_VINES || aboveMaterial == Material.TWISTING_VINES_PLANT) {
            handleBlockDestroy(
                    broken,
                    blockCache,
                    BlockFace.DOWN,
                    m -> m == Material.TWISTING_VINES || m == Material.TWISTING_VINES_PLANT,
                    leavesConfig
            );
        }
    }

    private static void handleBlockDestroy(
            Block broken,
            BlockCache blockCache,
            BlockFace supportingFace,
            Predicate<Material> materialPredicate,
            LeavesConfig leavesConfig
    ) {
        final BlockFace iterationFace = supportingFace.getOppositeFace();
        final World world = broken.getWorld();
        Block iterated = broken.getRelative(iterationFace);
        while (materialPredicate.test(iterated.getType())) {
            final Location location = iterated.getLocation();
            final Position position = Position.fromLocation(location);
            final BlockData blockData = blockCache.removeBlockData(position);
            if (blockData != BlockData.EMPTY) {
                LeafDropUtil.addToDropPositions(blockCache, position, blockData);
            }
            iterated = iterated.getRelative(iterationFace);
        }
    }

    public static int countStack(
            Position position,
            BlockData blockData,
            BlockCache blockCache
    ) {
        final Set<BlockFace> faces = new HashSet<>();
        if (blockData instanceof CaveVineData) {
            faces.add(BlockFace.UP);
        } else if (blockData instanceof final AgeableData ageableData) {
            faces.addAll(ageableData.supportableFaces());
        } else {
            return 0;
        }
        int stack = 0;
        for (BlockFace face : faces) {
            Block iterated = position.toLocation().getBlock().getRelative(face);
            BlockData current;
            while ((current = blockCache.getBlockData(Position.fromLocation(iterated.getLocation()))).id().equals(blockData.id())) {
                stack++;
                iterated = iterated.getRelative(face);
            }
        }
        return stack;
    }

}
