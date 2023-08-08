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
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public record SaplingData(
        String id,
        int sendBlockId,
        Material realBlockType,
        List<String> schematicFiles,
        boolean randomPasteRotation,
        String modelPath,
        @Nullable BlockDataSound blockDataSound
) implements BlockData {

    private static final Set<BlockFace> SUPPORTABLE_FACES = Set.of(BlockFace.DOWN);

    @Override
    public WrappedBlockState getNewState(@Nullable Material worldMaterial) {
        return WrappedBlockState.getByGlobalId(this.sendBlockId);
    }

    @Override
    public Material worldBlockType() {
        return this.realBlockType;
    }

    @Override
    public Sound placeSound() {
        return Sound.BLOCK_GRASS_PLACE;
    }

    @Override
    public boolean isWorldTypeSame(Material worldMaterial) {
        final Material sendMaterial = SpigotConversionUtil.toBukkitBlockData(this.getNewState(null)).getMaterial();
        return worldMaterial == sendMaterial || this.worldBlockType() == worldMaterial;
    }

    @Override
    public Material breakReplacement() {
        return Material.AIR;
    }

    @Override
    public boolean shouldSave() {
        return !this.id().equals(LeavesConfig.getDefaultSaplingStringId(this.realBlockType()));
    }

    @Override
    public Set<BlockFace> supportableFaces() {
        return SUPPORTABLE_FACES;
    }

}
