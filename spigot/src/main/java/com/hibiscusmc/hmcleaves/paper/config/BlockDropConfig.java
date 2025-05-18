package com.hibiscusmc.hmcleaves.paper.config;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class BlockDropConfig {

    private final boolean requiresShears;
    private final ItemStack sapling;
    private final ItemStack leavesItem;

    public BlockDropConfig(boolean requiresShears, @Nullable ItemStack sapling, @Nullable ItemStack leavesItem) {
        this.requiresShears = requiresShears;
        this.sapling = sapling;
        this.leavesItem = leavesItem;
    }

    public boolean requiresShears() {
        return this.requiresShears;
    }

    public @Nullable ItemStack copySapling() {
        if (this.sapling == null) {
            return null;
        }
        return this.sapling.clone();
    }

    public @Nullable ItemStack copyLeavesItem() {
        if (this.leavesItem == null) {
            return null;
        }
        return this.leavesItem.clone();
    }

}
