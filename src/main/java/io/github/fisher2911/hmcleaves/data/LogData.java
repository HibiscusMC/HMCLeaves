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
import io.github.fisher2911.hmcleaves.hook.Hooks;
import org.bukkit.Material;

public record LogData(
        String id,
        String strippedLogId,
        int sendBlockId,
        Material realBlockType,
        Material strippedBlockType,
        boolean stripped,
        int strippedSendBlockId
) implements BlockData {

    @Override
    public WrappedBlockState getNewState() {
        if (!this.stripped) {
            final Integer id = Hooks.getBlockId(this.id);
            if (id == null) return this.create(this.sendBlockId);
            return this.create(id);
        }
        final Integer id = Hooks.getBlockId(this.strippedLogId);
        if (id == null) return this.create(this.strippedSendBlockId);
        return this.create(id);
    }

    private WrappedBlockState create(int blockId) {
        return WrappedBlockState.getByGlobalId(blockId);
    }

}
