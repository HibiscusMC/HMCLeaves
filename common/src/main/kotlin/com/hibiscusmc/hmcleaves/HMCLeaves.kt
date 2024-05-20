package com.hibiscusmc.hmcleaves

import com.github.retrooper.packetevents.PacketEvents
import com.hibiscusmc.hmcleaves.command.HMCLeavesCommand
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.database.LeavesDatabase
import com.hibiscusmc.hmcleaves.hook.Hooks
import com.hibiscusmc.hmcleaves.listener.BlockListener
import com.hibiscusmc.hmcleaves.listener.BukkitListeners
import com.hibiscusmc.hmcleaves.listener.ChunkListener
import com.hibiscusmc.hmcleaves.packet.PacketListener
import com.hibiscusmc.hmcleaves.packet.mining.BlockBreakManager
import com.hibiscusmc.hmcleaves.packet.mining.BlockBreakModifier
import com.hibiscusmc.hmcleaves.user.UserManager
import com.hibiscusmc.hmcleaves.world.WorldManager
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class HMCLeaves : JavaPlugin() {

    val leavesConfig: LeavesConfig by lazy { LeavesConfig(this) }
    val worldManager = WorldManager()
    private lateinit var database: LeavesDatabase
    val blockBreakManager: BlockBreakManager by lazy { BlockBreakManager(plugin = this) }
    val userManager: UserManager by lazy { UserManager(this) }

    override fun onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().settings.reEncodeByDefault(true)
            .checkForUpdates(false)
            .debug(true)
            .bStats(true)
        PacketEvents.getAPI().load()
    }

    override fun onEnable() {
        this.leavesConfig.load()
        this.database = LeavesDatabase.createDatabase(this, this.leavesConfig.getDatabaseSettings().type)

        val pluginManager = this.server.pluginManager
        pluginManager.registerEvents(BukkitListeners(this), this)
        pluginManager.registerEvents(ChunkListener(this), this)

        PacketEvents.getAPI().eventManager.registerListener(PacketListener(this))
        PacketEvents.getAPI().init()

        this.registerCommands()

        this.database.init()

        // load hooks after all plugins have been loaded, cannot use
        // softdepend because HMCLeaves must load before the world loads
        Bukkit.getScheduler().runTaskLater(this, Runnable {
            Hooks.init()
        }, 1)
    }

    private fun registerCommands() {
        this.getCommand("hmcleaves")?.setExecutor(HMCLeavesCommand(this))
    }

    override fun onDisable() {
        PacketEvents.getAPI().terminate()
        for (world in Bukkit.getWorlds()) {
            this.database.saveWorld(world, true)
        }
    }

    fun getDatabase(): LeavesDatabase = this.database

}