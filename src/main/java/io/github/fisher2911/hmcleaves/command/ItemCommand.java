package io.github.fisher2911.hmcleaves.command;

import io.github.fisher2911.hmcleaves.Config;
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

public class ItemCommand implements CommandExecutor, TabExecutor {

    public static final String PERMISSION = "hmcleaves.command.item";

    private final Config config;

    public ItemCommand(Config config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) return true;
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "/item <id>");
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
            PDCUtil.setLeafDataItem(debugTool);
            player.getInventory().addItem(debugTool);
            player.sendMessage(ChatColor.GREEN + "Debug Tool added to your inventory.");
            return true;
        }
        final LeafItem item = this.config.getItem(args[0]);
        if (item == null) {
            sender.sendMessage(ChatColor.RED + "Item not found.");
            return true;
        }
        player.getInventory().addItem(item.itemStack());
        sender.sendMessage(ChatColor.GREEN + "Item added to inventory.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        final List<String> tabs = new ArrayList<>();
        if (args.length != 1) return null;
        for (String items : this.config.getLeafItems().keySet()) {
            if (items.startsWith(args[0])) {
                tabs.add(items);
            }
        }
        return tabs;
    }
}
