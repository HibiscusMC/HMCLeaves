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
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Supplier;

public record CaveVineData(
        String id,
        String withGlowBerryId,
        int sendBlockId,
        // if it's the last one in the stack, it will send this block id
        int tippedSendBlockId,
        boolean glowBerry,
        String modelPath,
        int stackLimit,
        Set<BlockFace> supportableFaces,
        @Nullable BlockDataSound blockDataSound,
        boolean shouldGrowBerries,
        @Nullable Supplier<ItemStack> berryItemSupplier
) implements BlockData, LimitedStacking {

    @Override
    public WrappedBlockState getNewState(@Nullable Material worldMaterial) {
        final WrappedBlockState state;
        if (worldMaterial == Material.CAVE_VINES_PLANT && LeavesConfig.getDefaultCaveVinesStringId(this.glowBerry).equals(this.getCurrentId())) {
            state = StateTypes.CAVE_VINES_PLANT.createBlockState();
        } else if (worldMaterial == Material.CAVE_VINES){
            state = WrappedBlockState.getByGlobalId(this.tippedSendBlockId);
        } else {
            state = WrappedBlockState.getByGlobalId(this.sendBlockId);
        }
        if (this.glowBerry) state.setBerries(true);
        return state;
    }

    @Override
    public Material realBlockType() {
        return this.worldBlockType();
    }

    @Override
    public Material worldBlockType() {
        return Material.CAVE_VINES;
    }

    @Override
    public Sound placeSound() {
        return Sound.BLOCK_CAVE_VINES_PLACE;
    }

    @Override
    public String withGlowBerryId() {
        return withGlowBerryId;
    }

    public String getCurrentId() {
        if (this.glowBerry) return this.withGlowBerryId;
        return this.id;
    }

    public CaveVineData withGlowBerry(boolean glowBerry) {
        if (glowBerry == this.glowBerry) return this;
        return new CaveVineData(
                this.id,
                this.withGlowBerryId,
                this.sendBlockId,
                this.tippedSendBlockId,
                glowBerry,
                this.modelPath,
                this.stackLimit,
                this.supportableFaces,
                this.blockDataSound,
                this.shouldGrowBerries,
                this.berryItemSupplier
        );
    }

    @Override
    public boolean isWorldTypeSame(Material material) {
        return Tag.CAVE_VINES.isTagged(material);
    }

    @Override
    public Material breakReplacement() {
        return Material.AIR;
    }

    @Override
    public boolean shouldSave() {
        return !this.getCurrentId().equals(LeavesConfig.getDefaultCaveVinesStringId(this.glowBerry()));
    }

    public @Nullable Supplier<ItemStack> getBerryItem() {
        return this.berryItemSupplier;
    }

}
