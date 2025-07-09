package com.hibiscusmc.hmcleaves.paper.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.config.LeavesConfig;
import com.hibiscusmc.hmcleaves.paper.hook.worldedit.WorldEditHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandAlias("leaves")
public final class LeavesCommand extends BaseCommand {

    public static final String SAVE_SCHEMATIC_PERMISSION = "hmcleaves.command.transformschematic";

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
    @CommandCompletion("@items @range:1-64")
    @CommandPermission(LeavesConfig.GIVE_ITEM_PERMISSION)
    public void onGive(Player player, String itemId, @Default("1") int amount) {
        if (amount <= 0) {
            player.sendMessage(Component.text("Amount must be greater than 0").color(NamedTextColor.RED));
            return;
        }
        if (itemId.equalsIgnoreCase("debug"))
            if (!this.config.isItemId(itemId)) {
                player.sendMessage(Component.text("Item ID not found").color(NamedTextColor.RED));
                return;
            }
        final ItemStack item = this.config.createItemStack(itemId);
        if (item == null) {
            player.sendMessage(Component.text("Item not found").color(NamedTextColor.RED));
            return;
        }
        item.setAmount(amount);
        player.getInventory().addItem(item);
        player.sendMessage(Component.text("Item given").color(NamedTextColor.GREEN));
    }

    @Subcommand("placedecayable")
    @CommandPermission(LeavesConfig.PLACE_DECAYABLE_PERMISSION)
    public void onPlaceDecayable(Player player) {
        final boolean placingDecayable = this.plugin.leavesConfig().isPlacingDecayable(player);
        if (placingDecayable) {
            this.plugin.leavesConfig().setPlacingDecayable(player, false);
            player.sendMessage(Component.text("Your placed leaves will not decay").color(NamedTextColor.GREEN));
        } else {
            this.plugin.leavesConfig().setPlacingDecayable(player, true);
            player.sendMessage(Component.text("Your placed leaves will now decay").color(NamedTextColor.GREEN));
        }
    }

    @Subcommand("transformschem")
    @CommandPermission(SAVE_SCHEMATIC_PERMISSION)
    public void onTransformSchem(Player player) {
        final WorldEditHook worldEditHook = this.plugin.worldEditHook();
        if (worldEditHook == null) {
            player.sendMessage(Component.text("WorldEdit hook is not available!").color(NamedTextColor.RED));
            return;
        }
        worldEditHook.trySaveSchematic(player);
        
    }

}

