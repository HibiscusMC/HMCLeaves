package com.hibiscusmc.hmcleaves;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.hibiscusmc.hmcleaves.config.LeavesConfig;
import com.hibiscusmc.hmcleaves.config.LeavesConfigImplementation;
import com.hibiscusmc.hmcleaves.database.LeavesDatabase;
import com.hibiscusmc.hmcleaves.database.SQLiteLeavesDatabase;
import com.hibiscusmc.hmcleaves.listener.WorldListener;
import com.hibiscusmc.hmcleaves.packet.LeavesPacketListener;
import com.hibiscusmc.hmcleaves.world.LeavesWorldManager;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;

public class HMCLeavesPlugin extends JavaPlugin implements HMCLeaves {

    private LeavesConfig<BlockData> leavesConfig;
    private LeavesDatabase leavesDatabase;
    private LeavesWorldManager leavesWorldManager;

    @Override
    public void onLoad() {

    }

    @Override
    public void onEnable() {
        final PacketEventsAPI<Plugin> api = SpigotPacketEventsBuilder.build(this);
        api.getSettings().checkForUpdates(false);
        api.getSettings().reEncodeByDefault(true);
        PacketEvents.setAPI(api);
        PacketEvents.getAPI().load();
        PacketEvents.getAPI().init();
        this.leavesWorldManager = new LeavesWorldManager(new HashMap<>());
        this.leavesConfig = new LeavesConfigImplementation(new HashMap<>(), new EnumMap<>(Material.class));
        this.leavesConfig.load();
        this.leavesDatabase = new SQLiteLeavesDatabase(
                this.getDataFolder().toPath().resolve("leaves.db"),
                this.leavesConfig
        );
        this.leavesDatabase.init();
        this.registerListeners();
        this.registerPacketListeners();
    }

    @Override
    public void onDisable() {

    }

    private void registerListeners() {
        List.of(new WorldListener(this)).forEach(listener -> Bukkit.getServer().getPluginManager().registerEvents(listener, this));
    }

    private void registerPacketListeners() {
        PacketEvents.getAPI().getEventManager().registerListener(new LeavesPacketListener(this.leavesWorldManager, this.leavesConfig));
    }

    @Override
    public LeavesConfig<BlockData> leavesConfig() {
        return this.leavesConfig;
    }

    @Override
    public LeavesWorldManager worldManager() {
        return this.leavesWorldManager;
    }

}
