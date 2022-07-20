package io.github.fisher2911.hmcleaves;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public record LeafItem(String id, ItemStack itemStack, Material leafType, int distance, boolean persistent) {

}
