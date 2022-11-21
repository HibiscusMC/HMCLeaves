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

package io.github.fisher2911.hmcleaves.util;

import com.jeff_media.customblockdata.CustomBlockData;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.nms.FakeLeafData;
import org.bukkit.Chunk;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.function.Supplier;

public class PDCHelper implements io.github.fisher2911.hmcleaves.nms.PDCHelper {

    private final Supplier<HMCLeaves> pluginSupplier;
    private HMCLeaves plugin;

    public PDCHelper(Supplier<HMCLeaves> pluginSupplier) {
        this.pluginSupplier = pluginSupplier;
    }

    @Override
    public boolean chunkHasPersistentData(PersistentDataContainer container) {
        return PDCUtil.hasLeafData(container);
    }

    @Override
    public void setDistance(PersistentDataContainer container, byte distance) {
        container.set(PDCUtil.DISTANCE_KEY, PersistentDataType.BYTE, distance);
    }

    @Override
    public void setPersistent(PersistentDataContainer container, byte persistent) {
        container.set(PDCUtil.PERSISTENCE_KEY, PersistentDataType.BYTE, persistent);
    }

    @Override
    public void setActualPersistent(PersistentDataContainer container, byte persistent) {
        container.set(PDCUtil.ACTUAL_PERSISTENCE_KEY, PersistentDataType.BYTE, persistent);
    }

    @Override
    public void setActualDistance(PersistentDataContainer container, byte distance) {
        container.set(PDCUtil.ACTUAL_DISTANCE_KEY, PersistentDataType.BYTE, distance);
    }

    @Override
    public void setChunkHasLeafData(PersistentDataContainer container, boolean hasLeafData) {
        PDCUtil.setHasLeafData(container);
    }

    @Override
    public boolean chunkHasLeafData(PersistentDataContainer container) {
        return PDCUtil.hasLeafData(container);
    }

    @Override
    public void removeLeafBlockData(CustomBlockData data) {
        data.remove(PDCUtil.DISTANCE_KEY);
        data.remove(PDCUtil.PERSISTENCE_KEY);
        data.remove(PDCUtil.ACTUAL_DISTANCE_KEY);
        data.remove(PDCUtil.ACTUAL_PERSISTENCE_KEY);
    }

    @Override
    public FakeLeafData getFakeLeafData(CustomBlockData data) {
        return PDCUtil.getFakeLeafData(data);
    }

    @Override
    public void handleChunkUnload(Chunk chunk) {
        this.plugin().getLeafCache().remove(chunk);
    }

    public HMCLeaves plugin() {
        if (this.plugin == null) {
            this.plugin = this.pluginSupplier.get();
        }
        return this.plugin;
    }

}
