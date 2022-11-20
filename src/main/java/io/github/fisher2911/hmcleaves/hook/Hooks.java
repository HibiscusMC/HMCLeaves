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
