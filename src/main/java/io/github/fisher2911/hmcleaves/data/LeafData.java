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

public record LeafData(
        String id,
        int sendBlockId,
        Material realBlockType,
        int displayDistance,
        boolean displayPersistence,
        boolean worldPersistence
) implements BlockData {

    @Override
    public Material worldBlockType() {
        return this.realBlockType;
    }

    @Override
    public WrappedBlockState getNewState() {
        final WrappedBlockState newState = WrappedBlockState.getByGlobalId(this.sendBlockId);
        newState.setDistance(this.displayDistance);
        newState.setPersistent(this.displayPersistence);
        return newState;
    }

    @Override
    public Sound placeSound() {
        return Sound.BLOCK_GRASS_PLACE;
    }

}
