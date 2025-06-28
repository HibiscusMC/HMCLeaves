package com.hibiscusmc.hmcleaves.paper.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.config.LeavesConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandAlias("leaves")
public final class LeavesCommand extends BaseCommand {

    private final HMCLeaves plugin;
    private final LeavesConfig config;

    public LeavesCommand(HMCLeaves plugin) {
        this.plugin = plugin;
        this.config = plugin.leavesConfig();
    }

    @HelpCommand
    public static void onHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("give")
    @CommandCompletion("@items")
    @CommandPermission(LeavesConfig.GIVE_ITEM_PERMISSION)
    public void onGive(Player player, String itemId, int amount) {
        if (amount <= 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Amount must be greater than 0"));
            return;
        }
        if (itemId.equalsIgnoreCase("debug"))
            if (!this.config.isItemId(itemId)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Item ID not found"));
                return;
            }
        final ItemStack item = this.config.createItemStack(itemId);
        if (item == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Item not found"));
            return;
        }
        item.setAmount(amount);
        player.getInventory().addItem(item);
        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Item given"));
    }

    @Subcommand("placedecayable")
    @CommandPermission(LeavesConfig.PLACE_DECAYABLE_PERMISSION)
    public void onPlaceDecayable(Player player) {
        final boolean placingDecayable = this.plugin.leavesConfig().isPlacingDecayable(player);
        if (placingDecayable) {
            this.plugin.leavesConfig().setPlacingDecayable(player, false);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Your placed leaves will not decay"));
        } else {
            this.plugin.leavesConfig().setPlacingDecayable(player, true);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Your placed leaves will now decay"));
        }
    }

}

