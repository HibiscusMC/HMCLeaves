package com.hibiscusmc.hmcleaves.paper;

import co.aikar.commands.PaperCommandManager;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import com.hibiscusmc.hmcleaves.paper.database.sql.SQLiteLeavesDatabase;
import com.hibiscusmc.hmcleaves.paper.hook.worldedit.WorldEditHook;
import com.hibiscusmc.hmcleaves.paper.listener.BlockBreakListener;
import com.hibiscusmc.hmcleaves.paper.listener.BlockBurnListener;
import com.hibiscusmc.hmcleaves.paper.listener.BlockChangeListener;
import com.hibiscusmc.hmcleaves.paper.listener.BlockDecayListener;
import com.hibiscusmc.hmcleaves.paper.listener.BlockDestroyListener;
import com.hibiscusmc.hmcleaves.paper.listener.BlockDropItemListener;
import com.hibiscusmc.hmcleaves.paper.listener.BlockExplodeListener;
import com.hibiscusmc.hmcleaves.paper.listener.BlockPistonListener;
import com.hibiscusmc.hmcleaves.paper.listener.BlockPlaceListener;
import com.hibiscusmc.hmcleaves.paper.listener.BlockWaterlogListener;
import com.hibiscusmc.hmcleaves.paper.listener.CustomBlockListener;
import com.hibiscusmc.hmcleaves.paper.listener.TreeGrowListener;
import com.hibiscusmc.hmcleaves.paper.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.paper.world.LeavesChunkSection;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorld;
import com.hibiscusmc.hmcleaves.nms.NMSHandler;
import com.hibiscusmc.hmcleaves.paper.breaking.BlockBreakManager;
import com.hibiscusmc.hmcleaves.paper.command.LeavesCommand;
import com.hibiscusmc.hmcleaves.paper.config.LeavesConfig;
import com.hibiscusmc.hmcleaves.paper.database.LeavesDatabase;
import com.hibiscusmc.hmcleaves.paper.hook.item.ItemHook;
import com.hibiscusmc.hmcleaves.paper.hook.item.nexo.NexoItemHook;
import com.hibiscusmc.hmcleaves.paper.listener.WorldListener;
import com.hibiscusmc.hmcleaves.paper.packet.LeavesPacketListener;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorldManager;
import com.hibiscusmc.hmcleaves.paper.packet.PlayerDigListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;

public class HMCLeaves extends JavaPlugin {

    private LeavesConfig leavesConfig;
    private LeavesDatabase leavesDatabase;
    private LeavesWorldManager leavesWorldManager;
    private Path configPath;
    private ItemHook itemHook;
    private WorldEditHook worldEditHook;
    private BlockBreakManager breakManager;
    private NMSHandler nmsHandler;

    @Override
    public void onLoad() {
        this.configPath = this.getDataFolder().toPath().resolve("config.yml");
        if (!this.configPath.toFile().exists()) {
            this.saveResource("config.yml", false);
        }
        this.leavesConfig = new LeavesConfig(this, this.configPath, new HashMap<>(), new EnumMap<>(Material.class), new HashMap<>(), new HashMap<>());
        this.leavesConfig.load();
        this.initializeNMSHandler();
    }

    @Override
    public void onEnable() {
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
        this.leavesDatabase = new SQLiteLeavesDatabase(this, this.getDataFolder().toPath().resolve("data"), this.leavesConfig);
        this.leavesDatabase.init();
        if (this.getServer().getPluginManager().getPlugin("Nexo") != null) {
            this.itemHook = new NexoItemHook(this);
        } else {
            this.itemHook = ItemHook.EMPTY;
        }
        this.breakManager = new BlockBreakManager(this);
        this.registerListeners();
        this.registerPacketListeners();
        this.registerCommands();

        final PluginManager pluginManager = this.getServer().getPluginManager();
        if (pluginManager.getPlugin("WorldEdit") != null || pluginManager.getPlugin("FastAsyncWorldEdit") != null) {
            this.worldEditHook = new WorldEditHook(this);
            this.worldEditHook.load();
        }
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
        if (this.itemHook instanceof final Listener listener) {
            Bukkit.getServer().getPluginManager().registerEvents(listener, this);
        }
        List.of(
                new WorldListener(this),
                this.createListener(BlockBreakListener::new),
                this.createListener(BlockBurnListener::new),
                this.createListener(BlockDecayListener::new),
                this.createListener(BlockDestroyListener::new),
                this.createListener(BlockDropItemListener::new),
                this.createListener(BlockExplodeListener::new),
                this.createListener(BlockPistonListener::new),
                this.createListener(BlockPlaceListener::new),
                this.createListener(BlockWaterlogListener::new),
                this.createListener(TreeGrowListener::new),
                this.createListener(BlockChangeListener::new)
        ).forEach(listener -> Bukkit.getServer().getPluginManager().registerEvents(listener, this));
    }

    private Listener createListener(Function<HMCLeaves, CustomBlockListener> constructor) {
        return constructor.apply(this);
    }

    private void registerPacketListeners() {
        final EventManager eventManager = PacketEvents.getAPI().getEventManager();
        eventManager.registerListener(new LeavesPacketListener(this.leavesWorldManager, this.leavesConfig));
        if (this.leavesConfig.handleMining()) {
            eventManager.registerListener(new PlayerDigListener(this.breakManager, this.leavesWorldManager, this.leavesConfig));
        }
    }

    private void registerCommands() {
        final PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.getCommandCompletions().registerCompletion("items", context -> this.leavesConfig.itemIds());
        commandManager.registerCommand(new LeavesCommand(this));
    }

    private void initializeNMSHandler() {
        try {
            final String version = Bukkit.getServer().getVersion();
            if (version.contains("1.21")) {
                this.createNMSHandler("v1_21_4");
            } else if (version.contains("1.20.5") || version.contains("1.20.6")) {
                this.createNMSHandler("v1_20_6");
            } else if (version.contains("1.20.4") || version.contains("1.20.3")) {
                this.createNMSHandler("v1_20_3");
            } else if (version.contains("1.20")) {
                this.createNMSHandler("v1_20");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNMSHandler(String packageVersion) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException {
        this.nmsHandler = (NMSHandler) Class.forName("com.hibiscusmc.hmcleaves." + packageVersion + ".NMSHandler").getConstructors()[0]
                .newInstance();
    }

    public LeavesConfig leavesConfig() {
        return this.leavesConfig;
    }

    public LeavesWorldManager worldManager() {
        return this.leavesWorldManager;
    }

    public LeavesDatabase database() {
        return this.leavesDatabase;
    }

    public ItemHook itemHook() {
        return this.itemHook;
    }

    public @Nullable WorldEditHook worldEditHook() {
        return this.worldEditHook;
    }

    public NMSHandler nmsHandler() {
        return this.nmsHandler;
    }

    public void log(Level level, String message) {
        if (!this.leavesConfig.debugMode() && level.intValue() <= Level.INFO.intValue()) {
            return;
        }
        this.getLogger().log(level, message);
    }

}
