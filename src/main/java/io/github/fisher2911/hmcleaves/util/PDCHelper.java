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
        //                    blockData.set(PDCUtil.DISTANCE_KEY, PersistentDataType.BYTE, (byte) data.state().getDistance());
//                    blockData.set(PDCUtil.PERSISTENCE_KEY, PersistentDataType.BYTE, (byte) (data.state().isPersistent() ? 1 : 0));
//                    blockData.set(PDCUtil.ACTUAL_PERSISTENCE_KEY, PersistentDataType.BYTE, (byte) (data.actuallyPersistent() ? 1 : 0));
//                    blockData.set(PDCUtil.ACTUAL_DISTANCE_KEY, PersistentDataType.BYTE, (byte) data.actualDistance());
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
