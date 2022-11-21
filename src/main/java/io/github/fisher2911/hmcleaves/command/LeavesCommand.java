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

import io.github.fisher2911.hmcleaves.Config;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.LeafItem;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LeavesCommand implements CommandExecutor, TabExecutor {

    public static final String PERMISSION = "hmcleaves.command.item";

    private final HMCLeaves plugin;
    private final Config config;

    public LeavesCommand(HMCLeaves plugin) {
        this.plugin = plugin;
        this.config = this.plugin.config();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) return true;
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "/hmcleaves give <id>");
            return true;
        }
        if (!(sender instanceof final Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args[0].equalsIgnoreCase("debugtool")) {
            final ItemStack debugTool = new ItemStack(Material.STICK);
            final ItemMeta itemMeta = debugTool.getItemMeta();
            itemMeta.setDisplayName(ChatColor.RED + "Debug Tool");
            debugTool.setItemMeta(itemMeta);
            PDCUtil.setLeafDataItem(debugTool, Config.DEBUG_TOOL_ID);
            player.getInventory().addItem(debugTool);
            player.sendMessage(ChatColor.GREEN + "Debug Tool added to your inventory.");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            this.plugin.reload();
            sender.sendMessage(ChatColor.GREEN + "Reloaded config.");
            return true;
        }
        if (args[0].equalsIgnoreCase("give")) {
            final LeafItem item = this.config.getItem(args[1]);
            if (item == null) {
                sender.sendMessage(ChatColor.RED + "Item not found.");
                return true;
            }
            player.getInventory().addItem(item.itemStack());
            sender.sendMessage(ChatColor.GREEN + "Item added to inventory.");
            return true;
        }
        player.sendMessage(ChatColor.RED + "Usage: /hmcleaves give <id>");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        final List<String> tabs = new ArrayList<>();
        if (args.length < 1) return tabs;
        final String arg = args[0];
        if (args.length == 1) {
            if ("reload".startsWith(arg)) tabs.add("reload");
            if ("give".startsWith(arg)) tabs.add("give");
            if ("debugtool".startsWith(arg)) tabs.add("debugtool");
        }
        if (args.length == 2 && arg.equalsIgnoreCase("give")) {
            final String itemArg = args[1];
            for (String items : this.config.getLeafItems().keySet()) {
                if (items.startsWith(itemArg)) {
                    tabs.add(items);
                }
            }
        }
        return tabs;
    }
}
