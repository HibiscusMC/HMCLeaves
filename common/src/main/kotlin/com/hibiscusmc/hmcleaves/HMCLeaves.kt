package com.hibiscusmc.hmcleaves

import com.github.retrooper.packetevents.PacketEvents
import com.hibiscusmc.hmcleaves.block.BlockChecker
import com.hibiscusmc.hmcleaves.command.HMCLeavesCommand
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.database.LeavesDatabase
import com.hibiscusmc.hmcleaves.debug.ActiveLeavesLogger
import com.hibiscusmc.hmcleaves.debug.DisabledLeavesLogger
import com.hibiscusmc.hmcleaves.debug.LeavesLogger
import com.hibiscusmc.hmcleaves.hook.Hooks
import com.hibiscusmc.hmcleaves.listener.BukkitListeners
import com.hibiscusmc.hmcleaves.listener.ChunkListener
import com.hibiscusmc.hmcleaves.listener.SoundListener
import com.hibiscusmc.hmcleaves.nms.NMSHandler
import com.hibiscusmc.hmcleaves.packet.PacketListener
import com.hibiscusmc.hmcleaves.packet.mining.BlockBreakManager
import com.hibiscusmc.hmcleaves.user.UserManager
import com.hibiscusmc.hmcleaves.world.WorldManager
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.lang.reflect.InvocationTargetException


class HMCLeaves : JavaPlugin() {

    val leavesConfig: LeavesConfig by lazy { LeavesConfig(this) }
    val worldManager = WorldManager()
    private lateinit var database: LeavesDatabase
    val blockBreakManager: BlockBreakManager by lazy { BlockBreakManager(plugin = this) }
    val userManager: UserManager by lazy { UserManager(this) }
    private lateinit var leavesLogger: LeavesLogger
    private val blockChecker = BlockChecker(this, worldManager)
    private lateinit var nmsHandler: NMSHandler

    override fun onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().settings.reEncodeByDefault(true)
            .checkForUpdates(false)
            .debug(true)
        PacketEvents.getAPI().load()
        if (Bukkit.getVersion().contains("1.19")) {
            this.nmsHandler = this.createNMSHandler("v1_19")
        }
        if (Bukkit.getVersion().contains("1.20.4")) {
            this.nmsHandler = this.createNMSHandler("v1_20_4")
        }
        if (Bukkit.getVersion().contains("1.21")) {
            this.nmsHandler = this.createNMSHandler("v1_21")
        }
    }

    @Throws(
        ClassNotFoundException::class,
        IllegalAccessException::class,
        InstantiationException::class,
        InvocationTargetException::class
    )
    private fun createNMSHandler(packageVersion: String): NMSHandler {
        return Class.forName("com.hibiscusmc.hmcleaves.${packageVersion}.NMSHandler").constructors[0]
            .newInstance() as NMSHandler
    }

    override fun onEnable() {
        this.leavesConfig.load()
        this.leavesLogger = if (this.leavesConfig.sendDebugMessages()) {
            ActiveLeavesLogger(this)
        } else {
            DisabledLeavesLogger
        }
        this.leavesLogger.init()
        this.database = LeavesDatabase.createDatabase(this, this.leavesConfig.getDatabaseSettings().type)

        val pluginManager = this.server.pluginManager
        pluginManager.registerEvents(BukkitListeners(this), this)
        pluginManager.registerEvents(ChunkListener(this), this)
        pluginManager.registerEvents(SoundListener(this), this)

        PacketEvents.getAPI().eventManager.registerListener(PacketListener(this, this.blockChecker))
        PacketEvents.getAPI().init()

        this.registerCommands()

        this.database.init()

        this.blockChecker.start()

        // load hooks after all plugins have been loaded, cannot use
        // softdepend because HMCLeaves must load before the world loads
        Bukkit.getScheduler().runTaskLater(this, Runnable {
            Hooks.init()
            // load textures after hooks have been initialized
            Hooks.reloadTextures()
            this.leavesConfig.loadTextures()
        }, 1)
    }

    private fun registerCommands() {
        this.getCommand("hmcleaves")?.setExecutor(HMCLeavesCommand(this))
    }

    override fun onDisable() {
        this.blockChecker.stop()
        this.leavesLogger.sendSynced(false)
        PacketEvents.getAPI().terminate()
        for (world in Bukkit.getWorlds()) {
            this.database.saveWorld(world, true)
        }
    }

    fun reload() {
        this.leavesConfig.reload()
        this.worldManager.reload(this.leavesConfig)
    }

    fun getDatabase(): LeavesDatabase = this.database

    fun getLeavesLogger() = this.leavesLogger

    fun getBlockChecker() = this.blockChecker

    fun getNMSHandler() = this.nmsHandler

}