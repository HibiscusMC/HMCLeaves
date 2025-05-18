package com.hibiscusmc.hmcleaves.paper;

import co.aikar.commands.PaperCommandManager;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.hibiscusmc.hmcleaves.common.HMCLeaves;
import com.hibiscusmc.hmcleaves.common.database.json.JsonLeavesDatabase;
import com.hibiscusmc.hmcleaves.common.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.common.world.LeavesChunkSection;
import com.hibiscusmc.hmcleaves.common.world.LeavesWorld;
import com.hibiscusmc.hmcleaves.paper.command.LeavesCommand;
import com.hibiscusmc.hmcleaves.paper.config.LeavesConfigImplementation;
import com.hibiscusmc.hmcleaves.common.database.LeavesDatabase;
import com.hibiscusmc.hmcleaves.common.database.sql.SQLiteLeavesDatabase;
import com.hibiscusmc.hmcleaves.paper.listener.LeavesListener;
import com.hibiscusmc.hmcleaves.paper.listener.WorldListener;
import com.hibiscusmc.hmcleaves.paper.packet.LeavesPacketListener;
import com.hibiscusmc.hmcleaves.common.world.LeavesWorldManager;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class HMCLeavesPlugin extends JavaPlugin implements HMCLeaves {

    private LeavesConfigImplementation leavesConfig;
    private LeavesDatabase leavesDatabase;
    private LeavesWorldManager leavesWorldManager;
    private Path configPath;

    @Override
    public void onLoad() {
        final PacketEventsAPI<Plugin> api = SpigotPacketEventsBuilder.build(this);
        api.getSettings().checkForUpdates(false)
                .reEncodeByDefault(true);
        PacketEvents.setAPI(api);
        PacketEvents.getAPI().load();
        this.configPath = this.getDataFolder().toPath().resolve("config.yml");
        if (!this.configPath.toFile().exists()) {
            this.saveResource("config.yml", false);
        }
        this.leavesConfig = new LeavesConfigImplementation(this, this.configPath, new HashMap<>(), new EnumMap<>(Material.class), new HashMap<>(), new HashMap<>(), new HashMap<>());
        this.leavesConfig.load();
    }

    @Override
    public void onEnable() {
        PacketEvents.getAPI().init();
        this.leavesWorldManager = new LeavesWorldManager(
                new ConcurrentHashMap<>(),
                worldId -> new LeavesWorld(
                        worldId,
                        new ConcurrentHashMap<>(),
                        chunkPosition -> new LeavesChunk(
                                chunkPosition,
                                new ConcurrentHashMap<>(),
                                position -> new LeavesChunkSection(position, new ConcurrentHashMap<>())
                        )
                )
        );
        this.leavesDatabase = new JsonLeavesDatabase(
                this,
                this.getDataFolder().toPath().resolve("data")
        );
        this.leavesDatabase.init();
        this.registerListeners();
        this.registerPacketListeners();
        this.registerCommands();
    }

    @Override
    public void onDisable() {
        for (World world : Bukkit.getWorlds()) {
            final var leavesWorld = this.leavesWorldManager.getWorld(world.getUID());
            if (leavesWorld != null) {
                for (LeavesChunk chunk : leavesWorld.getChunks().values()) {
                    if (!chunk.isDirty()) {
                        continue;
                    }
                    try {
                        chunk.setDirty(false);
                        this.leavesDatabase.saveChunk(chunk);
                    } catch (SQLException e) {
                        this.getLogger().severe("Failed to save chunk " + chunk.chunkPosition() + " in world " + world.getName());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void registerListeners() {
        List.of(
                        new WorldListener(this),
                        new LeavesListener(this)
                )
                .forEach(listener -> Bukkit.getServer().getPluginManager().registerEvents(listener, this));
    }

    private void registerPacketListeners() {
        PacketEvents.getAPI().getEventManager().registerListener(new LeavesPacketListener(this.leavesWorldManager, this.leavesConfig));
    }

    private void registerCommands() {
        final PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.getCommandCompletions().registerCompletion("items", context -> this.leavesConfig.itemIds());
        commandManager.registerCommand(new LeavesCommand(this));
    }

    @Override
    public LeavesConfigImplementation leavesConfig() {
        return this.leavesConfig;
    }

    @Override
    public LeavesWorldManager worldManager() {
        return this.leavesWorldManager;
    }

    @Override
    public LeavesDatabase database() {
        return this.leavesDatabase;
    }

    @Override
    public void log(Level level, String message) {
        this.getLogger().log(level, message);
    }
}
