package io.github.fisher2911.hmcleaves.hook;

import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.hook.itemsadder.ItemsAdderHook;
import io.github.fisher2911.hmcleaves.hook.oraxen.OraxenHook;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class Hooks {

    @Nullable
    private static ItemHook itemHook;

    public static void load(HMCLeaves plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Oraxen") != null) {
            plugin.getLogger().info("Oraxen found, loading hook");
            itemHook = new OraxenHook();
            plugin.getServer().getPluginManager().registerEvents(itemHook, plugin);
        }
        if (plugin.getServer().getPluginManager().getPlugin("ItemsAdder") != null) {
            plugin.getLogger().info("ItemsAdder found, loading hook");
            itemHook = new ItemsAdderHook();
            plugin.getServer().getPluginManager().registerEvents(itemHook, plugin);
        }
    }

    @Nullable
    public static String getItemId(ItemStack itemStack) {
        return itemHook == null ? null : itemHook.getId(itemStack);
    }

    @Nullable
    public static ItemStack getItem(String id) {
        return itemHook == null ? null : itemHook.getItem(id);
    }

}
