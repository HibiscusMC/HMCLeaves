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
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.List;

public record SaplingData(
        String id,
        int sendBlockId,
        Material realBlockType,
        List<String> schematicFiles,
        boolean randomPasteRotation
) implements BlockData {

    @Override
    public WrappedBlockState getNewState() {
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

}
