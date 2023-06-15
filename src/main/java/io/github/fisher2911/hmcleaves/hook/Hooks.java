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
import io.github.fisher2911.hmcleaves.data.SaplingData;
import io.github.fisher2911.hmcleaves.hook.hmcleaves.HMCLeavesHook;
import io.github.fisher2911.hmcleaves.hook.itemsadder.ItemsAdderHook;
import io.github.fisher2911.hmcleaves.hook.oraxen.OraxenHook;
import io.github.fisher2911.hmcleaves.hook.worldedit.WorldEditHook;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Axis;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class Hooks {

    @Nullable
    private static ItemHook itemHook;
    @Nullable
    private static WorldEditHook worldEditHook;

    public static void load(HMCLeaves plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Oraxen") != null) {
            plugin.getLogger().info("Oraxen found, loading hook");
            itemHook = new OraxenHook(plugin);
            plugin.getServer().getPluginManager().registerEvents(itemHook, plugin);
        }
        if (plugin.getServer().getPluginManager().getPlugin("ItemsAdder") != null) {
            plugin.getLogger().info("ItemsAdder found, loading hook");
            itemHook = new ItemsAdderHook(plugin);
            plugin.getServer().getPluginManager().registerEvents(itemHook, plugin);
        }
        if (itemHook == null) {
            itemHook = new HMCLeavesHook(plugin);
        }
        if (plugin.getServer().getPluginManager().getPlugin("WorldEdit") != null) {
            plugin.getLogger().info("WorldEdit found, loading hook");
            worldEditHook = new WorldEditHook(plugin);
            worldEditHook.load();
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

    @Nullable
    public static Integer getBlockId(String id) {
        return itemHook == null ? null : itemHook.getBlockId(id);
    }

    public static void trySaveSchematic(Player player) {
        if (worldEditHook == null) return;
        worldEditHook.trySaveSchematic(player);
    }

    public static void pasteSaplingSchematic(SaplingData saplingData, Position position) {
        if (worldEditHook == null) return;
        worldEditHook.pasteSaplingSchematic(saplingData, position);
    }

    public static boolean hasOtherItemHook() {
        return !(itemHook instanceof HMCLeavesHook);
    }

}
