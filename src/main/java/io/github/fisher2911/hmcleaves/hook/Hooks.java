package io.github.fisher2911.hmcleaves.hook;

import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.hook.oraxen.CustomLeafMechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.inventory.ItemStack;

public class Hooks {

    private static ItemsAdderHook itemsAdderHook;

    public static void load(HMCLeaves plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Oraxen") != null) {
            plugin.getLogger().info("Oraxen found, loading hook");
            MechanicsManager.registerMechanicFactory("customleaf",
                    CustomLeafMechanicFactory::new);
        }
        if (plugin.getServer().getPluginManager().getPlugin("ItemsAdder") != null) {
            plugin.getLogger().info("ItemsAdder found, loading hook");
            itemsAdderHook = new ItemsAdderHook();
        }
    }

    public static String getItemId(ItemStack itemStack) {
        return itemsAdderHook == null ? null : itemsAdderHook.getId(itemStack);
    }

}
