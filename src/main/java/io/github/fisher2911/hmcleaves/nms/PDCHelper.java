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
