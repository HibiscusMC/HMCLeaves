package io.github.fisher2911.hmcleaves;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public record LeafItem(String id, ItemStack itemStack, LeafData leafData) {

    public LeafItem(String id, ItemStack itemStack, Material material, int distance, boolean persistent, boolean actuallyPersistent) {
        this(id, itemStack, new LeafData(material, distance, persistent, actuallyPersistent));
    }

    @Override
    public String toString() {
        return "LeafItem{" +
                "id='" + id + '\'' +
                ", itemStack=" + itemStack +
                ", leafData=" + leafData +
                '}';
    }

}
