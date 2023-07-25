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
import io.github.fisher2911.hmcleaves.packet.BlockBreakModifier;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface BlockData {


    BlockData EMPTY = new BlockData() {

        private static final String EMPTY_ID = "empty";

        @Override
        public String id() {
            return EMPTY_ID;
        }

        @Override
        public int sendBlockId() {
            return 0;
        }

        @Override
        public Material realBlockType() {
            return Material.AIR;
        }

        @Override
        public Material worldBlockType() {
            return this.realBlockType();
        }

        @Override
        @Nullable
        public WrappedBlockState getNewState(@Nullable Material worldMaterial) {
            return null;
        }

        @Override
        @Nullable
        public Sound placeSound() {
            return null;
        }

        @Override
        @Nullable
        public String modelPath() {
            return null;
        }

        @Override
        public boolean isWorldTypeSame(Material worldMaterial) {
            return false;
        }

        @Override
        public Material breakReplacement() {
            return Material.AIR;
        }

        @Override
        public Set<BlockFace> supportableFaces() {
            return Set.of();
        }

        @Override
        public boolean shouldSave() {
            return false;
        }

        @Override
        public @Nullable BlockDataSound blockDataSound() {
            return null;
        }
    };

    String id();

    int sendBlockId();

    @Nullable
    WrappedBlockState getNewState(@Nullable Material worldMaterial);

    Material realBlockType();

    Material worldBlockType();

    @Nullable
    Sound placeSound();

    @Nullable
    String modelPath();

    boolean isWorldTypeSame(Material worldMaterial);

    Material breakReplacement();

    // faces that can support this block
    Set<BlockFace> supportableFaces();

    boolean shouldSave();

    @Nullable
    BlockDataSound blockDataSound();

    static LeafData leafData(
            String id,
            int sendBlockData,
            Material realBlockType,
            int displayDistance,
            boolean displayPersistence,
            boolean worldPersistence,
            boolean waterlogged,
            String modelPath,
            Set<BlockFace> supportableFaces,
            BlockBreakModifier blockBreakModifier,
            BlockDataSound blockDataSound
    ) {
        return new LeafData(
                id,
                sendBlockData,
                realBlockType,
                displayDistance,
                displayPersistence,
                worldPersistence,
                waterlogged,
                modelPath,
                supportableFaces,
                blockBreakModifier,
                blockDataSound
        );
    }

    static LogData logData(
            String id,
            String strippedLogId,
            int sendBlockId,
            Material realBlockType,
            Material strippedBlockType,
            boolean stripped,
            int strippedSendBlockId,
            Axis axis,
            String modelPath,
            Set<BlockFace> supportableFaces,
            BlockDataSound blockDataSound
    ) {
        return new LogData(
                id,
                strippedLogId,
                sendBlockId,
                realBlockType,
                strippedBlockType,
                stripped,
                strippedSendBlockId,
                axis,
                modelPath,
                supportableFaces,
                blockDataSound
        );
    }

    static SaplingData saplingData(
            String id,
            int sendBlockId,
            Material realBlockType,
            List<String> schematicFiles,
            boolean randomPasteRotation,
            String modelPath,
            BlockDataSound blockDataSound
    ) {
        return new SaplingData(
                id,
                sendBlockId,
                realBlockType,
                schematicFiles,
                randomPasteRotation,
                modelPath,
                blockDataSound
        );
    }

    static CaveVineData caveVineData(
            String id,
            String withGlowBerryId,
            int sendBlockId,
            int tipSendBlockId,
            boolean glowBerry,
            String modelPath,
            int stackLimit,
            Set<BlockFace> supportableFaces,
            BlockDataSound blockDataSound,
            boolean shouldGrowBerries,
            @Nullable Supplier<ItemStack> berryItemSupplier
    ) {
        return new CaveVineData(
                id,
                withGlowBerryId,
                sendBlockId,
                tipSendBlockId,
                glowBerry,
                modelPath,
                stackLimit,
                supportableFaces,
                blockDataSound,
                shouldGrowBerries,
                berryItemSupplier
        );
    }

    static AgeableData ageableData(
            String id,
            Material realBlockType,
            int sendBlockId,
            int tippedSendBlockId,
            String modelPath,
            Sound placeSound,
            Predicate<Material> worldTypeSamePredicate,
            Material defaultLowerMaterial,
            int stackLimit,
            Material breakReplacement,
            Set<BlockFace> supportableFaces,
            BlockDataSound blockDataSound
    ) {
        return new AgeableData(
                id,
                realBlockType,
                sendBlockId,
                tippedSendBlockId,
                modelPath,
                placeSound,
                worldTypeSamePredicate,
                defaultLowerMaterial,
                stackLimit,
                breakReplacement,
                supportableFaces,
                blockDataSound
        );
    }

}
