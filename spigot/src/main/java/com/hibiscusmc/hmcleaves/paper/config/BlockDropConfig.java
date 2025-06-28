package com.hibiscusmc.hmcleaves.paper.config;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class BlockDropConfig {

    private final boolean requiresShears;
    private final Supplier<@Nullable ItemStack> saplingSupplier;
    private final Supplier<@Nullable ItemStack> leavesItemSupplier;

    public BlockDropConfig(boolean requiresShears, Supplier<@Nullable ItemStack> saplingSupplier, Supplier<@Nullable ItemStack> leavesItemSupplier) {
        this.requiresShears = requiresShears;
        this.saplingSupplier = saplingSupplier;
        this.leavesItemSupplier = leavesItemSupplier;
    }

    public boolean requiresShears() {
        return this.requiresShears;
    }

    public @Nullable ItemStack copySapling() {
        return this.saplingSupplier.get();
    }

    public @Nullable ItemStack copyLeavesItem() {
        return this.leavesItemSupplier.get();
    }

}
