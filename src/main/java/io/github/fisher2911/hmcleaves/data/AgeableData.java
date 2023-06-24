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

package io.github.fisher2911.hmcleaves.data;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Predicate;

public record AgeableData(
        String id,
        Material realBlockType,
        int sendBlockId,
        String modelPath,
        Sound placeSound,
        // faces that can support this block
        Set<BlockFace> supportableFaces,
        Predicate<Material> worldTypeSamePredicate,
        Predicate<Block> placeAgainstPredicate,
        Material defaultLowerMaterial,
        int stackLimit,
        Material breakReplacement
) implements BlockData, LimitedStacking {

    @Override
    public WrappedBlockState getNewState(@Nullable Material worldMaterial) {
        final WrappedBlockState state;
        if (worldMaterial == this.defaultLowerMaterial && LeavesConfig.getDefaultAgeableStringId(this.realBlockType).equals(this.id)) {
            state = SpigotConversionUtil.fromBukkitBlockData(this.defaultLowerMaterial.createBlockData());
        } else {
            state = WrappedBlockState.getByGlobalId(this.sendBlockId);
        }
        return state;
//        return WrappedBlockState.getByGlobalId(this.sendBlockId);
    }

    public int getAge() {
        return WrappedBlockState.getByGlobalId(this.sendBlockId).getAge();
    }

    @Override
    public Material worldBlockType() {
        return this.realBlockType;
    }

    @Override
    public boolean isWorldTypeSame(Material worldMaterial) {
        return this.worldTypeSamePredicate.test(worldMaterial);
    }

    public boolean canBePlacedAgainst(Block block) {
        return this.placeAgainstPredicate.test(block);
    }

    @Override
    public boolean shouldSave() {
        return !this.id().equals(LeavesConfig.getDefaultAgeableStringId(this.realBlockType()));
    }

}
