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

package io.github.fisher2911.hmcleaves.nms;

import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.Chunk;
import org.bukkit.persistence.PersistentDataContainer;

public interface PDCHelper {

    boolean chunkHasPersistentData(PersistentDataContainer container);
    void setDistance(PersistentDataContainer container, byte distance);
    void setPersistent(PersistentDataContainer container, byte persistent);
    void setActualPersistent(PersistentDataContainer container, byte persistent);
    void setActualDistance(PersistentDataContainer container, byte distance);
    void setChunkHasLeafData(PersistentDataContainer container, boolean hasLeafData);
    boolean chunkHasLeafData(PersistentDataContainer container);
    void removeLeafBlockData(CustomBlockData data);
    FakeLeafData getFakeLeafData(CustomBlockData data);
    void handleChunkUnload(Chunk chunk);

}
