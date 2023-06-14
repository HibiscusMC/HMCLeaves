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

package io.github.fisher2911.hmcleaves.command;

import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.hook.Hooks;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class LeavesCommand implements TabExecutor {

    public static final String ITEM_PERMISSION = "hmcleaves.command.item";
    public static final String DEBUG_TOOL_PERMISSION = "hmcleaves.command.debugtool";
    public static final String RELOAD_PERMISSION = "hmcleaves.command.reload";
    public static final String SAVE_SCHEM_PERMISSION = "hmcleaves.command.transformschem";

    private static final String RELOAD_ARG = "reload";
    private static final String GIVE_ARG = "give";
    private static final String DEBUG_TOOL_ARG = "debugtool";
    private static final String TRANSFORM_SCHEM_ARG = "transformschem";

    private final HMCLeaves plugin;
    private final LeavesConfig leavesConfig;

    public LeavesCommand(HMCLeaves plugin) {
        this.plugin = plugin;
        this.leavesConfig = this.plugin.getLeavesConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender.hasPermission(ITEM_PERMISSION) && args.length == 0) {
            sender.sendMessage(ChatColor.RED + "/hmcleaves give <id>");
            return true;
        }
        if (!(sender instanceof final Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (sender.hasPermission(DEBUG_TOOL_PERMISSION) && args[0].equalsIgnoreCase(DEBUG_TOOL_ARG)) {
            final ItemStack debugTool = new ItemStack(Material.STICK);
            final ItemMeta itemMeta = debugTool.getItemMeta();
            itemMeta.setDisplayName(ChatColor.RED + "Debug Tool");
            debugTool.setItemMeta(itemMeta);
            PDCUtil.setItemId(debugTool, LeavesConfig.DEBUG_TOOL_ID);
            player.getInventory().addItem(debugTool);
            player.sendMessage(ChatColor.GREEN + "Debug Tool added to your inventory.");
            return true;
        }
        if (sender.hasPermission(RELOAD_PERMISSION) && args[0].equalsIgnoreCase(RELOAD_ARG)) {
            this.plugin.reload();
            sender.sendMessage(ChatColor.GREEN + "Reloaded config.");
            return true;
        }
        if (sender.hasPermission(ITEM_PERMISSION) && args[0].equalsIgnoreCase(GIVE_ARG)) {
            final Supplier<ItemStack> itemSupplier = this.leavesConfig.getItem(args[1]);
            final ItemStack itemStack;
            if (itemSupplier == null || (itemStack = itemSupplier.get()) == null) {
                sender.sendMessage(ChatColor.RED + "Item not found.");
                return true;
            }
            player.getInventory().addItem(itemStack);
            sender.sendMessage(ChatColor.GREEN + "Item added to inventory.");
            return true;
        }
        if (sender.hasPermission(SAVE_SCHEM_PERMISSION) && args[0].equalsIgnoreCase(TRANSFORM_SCHEM_ARG)) {
            Hooks.trySaveSchematic(player);
            return true;
        }
        if (!sender.hasPermission(ITEM_PERMISSION)) return true;
        player.sendMessage(ChatColor.RED + "Usage: /hmcleaves give <id>");
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        final List<String> tabs = new ArrayList<>();
        if (args.length < 1) return tabs;
        final String arg = args[0];
        if (args.length == 1) {
            if (sender.hasPermission(RELOAD_PERMISSION) && RELOAD_ARG.startsWith(arg)) tabs.add(RELOAD_ARG);
            if (sender.hasPermission(ITEM_PERMISSION) && GIVE_ARG.startsWith(arg)) tabs.add(GIVE_ARG);
            if (sender.hasPermission(DEBUG_TOOL_PERMISSION) && DEBUG_TOOL_ARG.startsWith(arg)) tabs.add(DEBUG_TOOL_ARG);
            if (sender.hasPermission(SAVE_SCHEM_PERMISSION) && TRANSFORM_SCHEM_ARG.startsWith(arg))
                tabs.add(TRANSFORM_SCHEM_ARG);
        }
        if (args.length == 2 && sender.hasPermission(ITEM_PERMISSION) && arg.equalsIgnoreCase(GIVE_ARG)) {
            final String itemArg = args[1];
            for (String items : this.leavesConfig.getPlayerItemIds()) {
                if (items.startsWith(itemArg)) {
                    tabs.add(items);
                }
            }
        }
        return tabs;
    }

}