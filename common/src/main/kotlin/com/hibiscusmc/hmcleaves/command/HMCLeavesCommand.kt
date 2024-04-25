package com.hibiscusmc.hmcleaves.command

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

private const val GIVE_COMMAND = "give"

class HMCLeavesCommand(
    private var plugin: HMCLeaves,
    private var config: LeavesConfig = plugin.getLeavesConfig()
) : CommandExecutor, TabExecutor {


    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.size < 2) {
            return true
        }
        when (args[0]) {
            GIVE_COMMAND -> {
                handleGive(sender as? Player ?: return true, args)
                return true
            }
        }
        return true
    }

    private fun handleGive(sender: Player, args: Array<String>) {
        val id = args[1]
        val data = this.config.getBlockData(id) ?: run {
            return
        }
        val item = data.createItem()
        sender.inventory.addItem(item)
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): MutableList<String> {
        val output = mutableListOf<String>()

        if (args.size <= 1) {
            output.addAll(
                listOf(
                    GIVE_COMMAND
                )
            )

            if (args.isEmpty()) {
                return output
            }
            return output.filter {
                it.startsWith(args[0])
            }.toMutableList()
        }

        return config.getBlockDataIds().filter {
            it.startsWith(args[1])
        }.toMutableList()
    }
}