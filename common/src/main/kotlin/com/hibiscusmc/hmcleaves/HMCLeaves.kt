package com.hibiscusmc.hmcleaves

import com.github.retrooper.packetevents.PacketEvents
import com.hibiscusmc.hmcleaves.command.HMCLeavesCommand
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.database.LeavesDatabase
import com.hibiscusmc.hmcleaves.database.UnsavedLeavesDatabase
import com.hibiscusmc.hmcleaves.listener.BlockListener
import com.hibiscusmc.hmcleaves.listener.BukkitListeners
import com.hibiscusmc.hmcleaves.packet.PacketListener
import com.hibiscusmc.hmcleaves.world.WorldManager
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.plugin.java.JavaPlugin

class HMCLeaves : JavaPlugin() {

    private val leavesConfig = LeavesConfig(this)
    private val worldManager = WorldManager()
    private val database = UnsavedLeavesDatabase(this)

    override fun onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().settings.reEncodeByDefault(true)
            .checkForUpdates(false)
            .debug(true)
            .bStats(true)
        PacketEvents.getAPI().load()
    }

    override fun onEnable() {
        logger.info("Enabling HMCLeaves")
        this.leavesConfig.load()

        this.server.pluginManager.registerEvents(BukkitListeners(this), this)

        PacketEvents.getAPI().eventManager.registerListener(PacketListener(this))
        PacketEvents.getAPI().init()

        this.registerCommands()
    }

    private fun registerCommands() {
        this.getCommand("hmcleaves")?.setExecutor(HMCLeavesCommand(this))
    }

    override fun onDisable() {
        PacketEvents.getAPI().terminate()
    }

    fun getLeavesConfig() = this.leavesConfig

    fun getWorldManager() = this.worldManager

    fun getDatabase(): LeavesDatabase = this.database
}