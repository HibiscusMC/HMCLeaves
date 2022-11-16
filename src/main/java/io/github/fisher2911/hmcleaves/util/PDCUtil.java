package io.github.fisher2911.hmcleaves.util;

import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.LeafData;
import io.github.fisher2911.hmcleaves.hook.Hooks;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public class PDCUtil {

    private static final HMCLeaves PLUGIN = HMCLeaves.getPlugin(HMCLeaves.class);
    public static final NamespacedKey DISTANCE_KEY = new NamespacedKey(PLUGIN, "leaf_data");
    public static final NamespacedKey PERSISTENCE_KEY = new NamespacedKey(PLUGIN, "leaf_persistence");
    public static final NamespacedKey ACTUAL_PERSISTENCE_KEY = new NamespacedKey(PLUGIN, "actual_leaf_persistence");
    public static final NamespacedKey ACTUAL_DISTANCE_KEY = new NamespacedKey(PLUGIN, "actual_leaf_distance");
    public static final NamespacedKey MATERIAL_KEY = new NamespacedKey(PLUGIN, "leaf_material");
    private static final NamespacedKey HAS_LEAF_DATA_KEY = new NamespacedKey(PLUGIN, "has_leaf_data");
    public static final NamespacedKey ITEM_KEY = new NamespacedKey(PLUGIN, "leaf_item");
    public static final NamespacedKey ITEM_DATA_KEY = new NamespacedKey(PLUGIN, "leaf_item_data");
    public static final NamespacedKey IS_TREE_BLOCK_KEY = new NamespacedKey(PLUGIN, "is_tree_block");

    public static boolean hasLeafData(PersistentDataContainer container) {
        final Byte data = container.get(HAS_LEAF_DATA_KEY, PersistentDataType.BYTE);
        return data != null && data == 1;
    }

    public static void setHasLeafData(PersistentDataContainer container) {
        container.set(HAS_LEAF_DATA_KEY, PersistentDataType.BYTE, (byte) 1);
    }

    public static boolean isLeafDataItem(ItemStack itemStack) {
        return getLeafDataItemId(itemStack) != null;
    }

    public static String getLeafDataItemId(ItemStack itemStack) {
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;
        final String id = meta.getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING);
        if (id == null) return Hooks.getItemId(itemStack);
        return id;
    }

    @Nullable
    public static LeafData getLeafData(PersistentDataContainer container) {
        final Byte distance = container.get(DISTANCE_KEY, PersistentDataType.BYTE);
        final Byte persistence = container.get(PERSISTENCE_KEY, PersistentDataType.BYTE);
        final Byte actualPersistence = container.get(ACTUAL_PERSISTENCE_KEY, PersistentDataType.BYTE);
        final String material = container.get(MATERIAL_KEY, PersistentDataType.STRING);
        if (distance == null || persistence == null || material == null || actualPersistence == null) return null;
        try {
            return new LeafData(Material.valueOf(material), distance, persistence == 1, actualPersistence == 1);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void setLeafDataItem(ItemStack itemStack, String id) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return;
        itemMeta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, id);
        itemStack.setItemMeta(itemMeta);
    }

    public static boolean isTreeBlock(PersistentDataContainer container) {
        final Byte data = container.get(IS_TREE_BLOCK_KEY, PersistentDataType.BYTE);
        return data != null && data == 1;
    }

    public static void setTreeBlock(PersistentDataContainer container) {
        container.set(IS_TREE_BLOCK_KEY, PersistentDataType.BYTE, (byte) 1);
    }

}
