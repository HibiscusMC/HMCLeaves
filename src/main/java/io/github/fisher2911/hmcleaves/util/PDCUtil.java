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

import io.github.fisher2911.hmcleaves.HMCLeaves;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PDCUtil {

    private static final byte CHUNK_VERSION = 4;

    public static final HMCLeaves PLUGIN = HMCLeaves.getPlugin(HMCLeaves.class);
    public static final NamespacedKey ITEM_ID_KEY = new NamespacedKey(PLUGIN, "item_id");
    public static final NamespacedKey HAS_LEAF_DATA_KEY = new NamespacedKey(PLUGIN, "has_leaf_data");

    // schematic data
    public static final NamespacedKey LEAF_ID_KEY = new NamespacedKey(PLUGIN, "leaf_id");
    public static final NamespacedKey LEAF_WORLD_DISTANCE_KEY = new NamespacedKey(PLUGIN, "leaf_world_distance");
    public static final NamespacedKey LEAF_WORLD_PERSISTENCE_KEY = new NamespacedKey(PLUGIN, "leaf_world_persistence");
    public static final NamespacedKey LOG_ID_KEY = new NamespacedKey(PLUGIN, "log_id");
    public static final NamespacedKey SAPLING_ID_KEY = new NamespacedKey(PLUGIN, "sapling_id");
    public static final NamespacedKey SAPLING_STAGE_KEY = new NamespacedKey(PLUGIN, "sapling_stage");

    @Nullable
    public static String getItemId(ItemStack itemStack) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return null;
        return itemMeta.getPersistentDataContainer().get(ITEM_ID_KEY, PersistentDataType.STRING);
    }


    public static void setItemId(ItemStack itemStack, String itemId) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return;
        itemMeta.getPersistentDataContainer().set(ITEM_ID_KEY, PersistentDataType.STRING, itemId);
        itemStack.setItemMeta(itemMeta);
    }

    public static boolean chunkHasLeafData(PersistentDataContainer container) {
        return Objects.equals(container.get(HAS_LEAF_DATA_KEY, PersistentDataType.BYTE), CHUNK_VERSION);
    }

    public static void setChunkHasLeafData(PersistentDataContainer container) {
        container.set(HAS_LEAF_DATA_KEY, PersistentDataType.BYTE, (byte) CHUNK_VERSION);
    }

}
