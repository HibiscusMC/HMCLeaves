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
import org.jetbrains.annotations.Nullable;

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
        @Nullable
        public WrappedBlockState getNewState() {
            return null;
        }
    };

    String id();

    int sendBlockId();

    @Nullable
    WrappedBlockState getNewState();

    Material realBlockType();

    static LeafData leafData(
            String id,
            int sendBlockData,
            Material realBlockType,
            int displayDistance,
            boolean displayPersistence
    ) {
        return new LeafData(
                id,
                sendBlockData,
                realBlockType,
                displayDistance,
                displayPersistence
        );
    }

    static LogData logData(
            String id,
            String strippedLogId,
            int sendBlockData,
            Material realBlockType,
            Material strippedBlockType,
            boolean stripped,
            int strippedSendBlockId
    ) {
        return new LogData(
                id,
                strippedLogId,
                sendBlockData,
                realBlockType,
                strippedBlockType,
                stripped,
                strippedSendBlockId
        );
    }


}
