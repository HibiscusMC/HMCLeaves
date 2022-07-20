package io.github.fisher2911.hmcleaves.util;

import io.github.fisher2911.hmcleaves.HMCLeaves;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PDCUtil {

    private static final HMCLeaves PLUGIN = HMCLeaves.getPlugin(HMCLeaves.class);
    public static final NamespacedKey DISTANCE_KEY = new NamespacedKey(PLUGIN, "leaf_data");
    public static final NamespacedKey PERSISTENCE_KEY = new NamespacedKey(PLUGIN, "leaf_persistence");
    private static final NamespacedKey HAS_LEAF_DATA_KEY = new NamespacedKey(PLUGIN, "has_leaf_data");
    public static final NamespacedKey ITEM_KEY = new NamespacedKey(PLUGIN, "leaf_item");

    public static boolean hasLeafData(PersistentDataContainer container) {
        final Byte data = container.get(HAS_LEAF_DATA_KEY, PersistentDataType.BYTE);
        return data != null && data == 1;
    }

    public static void setHasLeafData(PersistentDataContainer container) {
        container.set(HAS_LEAF_DATA_KEY, PersistentDataType.BYTE, (byte) 1);
    }
}
