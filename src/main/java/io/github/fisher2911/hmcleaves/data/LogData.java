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
import io.github.fisher2911.hmcleaves.hook.Hooks;
import io.github.fisher2911.hmcleaves.packet.BlockBreakManager;
import io.github.fisher2911.hmcleaves.packet.BlockBreakModifier;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

public record LogData(
        String id,
        String strippedLogId,
        int sendBlockId,
        Material realBlockType,
        Material strippedBlockType,
        boolean stripped,
        int strippedSendBlockId,
        Axis axis,
        Set<BlockFace> supportableFaces,
        @Nullable BlockDataSound blockDataSound
) implements BlockData, MineableData {

    @Override
    public Material worldBlockType() {
        if (this.stripped) return this.strippedBlockType;
        return this.realBlockType;
    }

    @Override
    public WrappedBlockState getNewState(@Nullable Material worldMaterial) {
        final int sendId = Objects.requireNonNullElse(
                Hooks.getBlockId(this.getCurrentId()), this.sendBlockId()
        );
        return this.create(sendId);
    }

    private WrappedBlockState create(int blockId) {
        final WrappedBlockState state = WrappedBlockState.getByGlobalId(blockId);
        state.setAxis(this.convertBlockAxis());
        return state;
    }

    private com.github.retrooper.packetevents.protocol.world.states.enums.Axis convertBlockAxis() {
        return switch (this.axis) {
            case X -> com.github.retrooper.packetevents.protocol.world.states.enums.Axis.X;
            case Y -> com.github.retrooper.packetevents.protocol.world.states.enums.Axis.Y;
            case Z -> com.github.retrooper.packetevents.protocol.world.states.enums.Axis.Z;
        };
    }

    public LogData strip() {
        return new LogData(
                this.id,
                this.strippedLogId,
                this.sendBlockId,
                this.realBlockType,
                this.strippedBlockType,
                true,
                this.strippedSendBlockId,
                this.axis,
                this.supportableFaces,
                this.blockDataSound
        );
    }

    @Override
    public Sound placeSound() {
        return Sound.BLOCK_WOOD_PLACE;
    }

    @Override
    public int sendBlockId() {
        if (this.stripped) return this.strippedSendBlockId;
        return this.sendBlockId;
    }

    public int getSendBlockId() {
        return this.sendBlockId;
    }

    public String getCurrentId() {
        if (this.stripped) return this.strippedLogId;
        return this.id;
    }

    @Override
    public @Nullable String modelPath() {
        return null;
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
        if (this.stripped()) {
            return !this.strippedLogId().equals(LeavesConfig.getDefaultStrippedLogStringId(this.strippedBlockType()));
        }
        return !this.id().equals(LeavesConfig.getDefaultLogStringId(this.realBlockType()));
    }

    @Override
    public BlockBreakModifier blockBreakModifier() {
        return BlockBreakManager.LOG_BREAK_MODIFIER;
    }

}
