package com.hibiscusmc.hmcleaves.command

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.hook.Hooks
import com.hibiscusmc.hmcleaves.util.MINI_MESAGE
import com.hibiscusmc.hmcleaves.util.parseAsAdventure
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

private data class CommandArg(val name: String, val permission: String? = null)

private val GIVE_COMMAND = CommandArg("give")
private val DEBUG_STICK_COMMAND = CommandArg("debugstick", "hmcleaves.command.debugstick")
private val RELOAD_COMMAND = CommandArg("reload", "hmcleaves.command.reload")
private val TRANSFORM_SCHEM_COMMAND = CommandArg("transformschem", "hmcleaves.command.transformschem");

private val COMMANDS = listOf(
    GIVE_COMMAND,
    DEBUG_STICK_COMMAND,
    RELOAD_COMMAND,
    TRANSFORM_SCHEM_COMMAND
)

class HMCLeavesCommand(
    private var plugin: HMCLeaves,
    private var config: LeavesConfig = plugin.leavesConfig
) : CommandExecutor, TabExecutor {


    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            return true
        }

        when (args[0]) {
            GIVE_COMMAND.name -> {
                if (args.size < 2) {
                    sender.sendMessage("<red>Usage: /hmcleaves give <item>".parseAsAdventure())
                    return true
                }
                handleGive(sender as? Player ?: return true, args)
                return true
            }

            DEBUG_STICK_COMMAND.name -> {
                if (sender !is Player) return true
                if (!sender.hasPermission(DEBUG_STICK_COMMAND.permission ?: return true)) return true
                sender.inventory.addItem(this.config.getDebugStick())
                return true
            }

            RELOAD_COMMAND.name -> {
                if (!sender.hasPermission(RELOAD_COMMAND.permission ?: return true)) return true
                this.config.reload()
                sender.sendMessage("${ChatColor.RED}Config successfully reloaded")
                return true
            }

            TRANSFORM_SCHEM_COMMAND.name -> {
                if (!sender.hasPermission(TRANSFORM_SCHEM_COMMAND.permission ?: return true)) return true
                Hooks.trySaveSchematic(sender as? Player ?: return true)
                return true
            }
        }
        return true
    }

    private fun handleGive(sender: Player, args: Array<String>) {
        val id = args[1]
        val data = this.config.getBlockData(id) ?: run {
            sender.sendMessage("${ChatColor.RED}No block data found for $id")
            return
        }
        val amount = if (args.size > 2) {
            try {
                args[2].toInt()
            } catch (exception: NumberFormatException) {
                1
            }
        } else {
            1
        }
        val item = data.createItem() ?: run {
            sender.sendMessage("${ChatColor.RED}No item found for $id")
            return
        }
        item?.amount = amount
        sender.inventory.addItem(item)
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): MutableList<String>? {
        val output = mutableListOf<String>()
        if (args.size <= 1) {
            output.addAll(COMMANDS.filter { cmd -> sender.hasPermission(cmd.permission ?: return@filter true) }
                .map { cmd -> cmd.name })

            if (args.isEmpty()) {
                return output
            }
            return output.filter {
                it.startsWith(args[0])
            }.toMutableList()
        }

        if (args.size == 2 && args[0] == GIVE_COMMAND.name) {
            return config.getBlockDataIds().filter {
                it.startsWith(args[1])
            }.toMutableList()
        }
        return null
    }
}