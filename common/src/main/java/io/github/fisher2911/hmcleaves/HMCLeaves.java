/*
 *
 *  *     HMCLeaves
 *  *     Copyright (C) 2022  Hibiscus Creative Studios
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU General Public License
 *  *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package io.github.fisher2911.hmcleaves;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import io.github.fisher2911.hmcleaves.cache.BlockCache;
import io.github.fisher2911.hmcleaves.cache.ChunkBlockCache;
import io.github.fisher2911.hmcleaves.cache.WorldBlockCache;
import io.github.fisher2911.hmcleaves.command.LeavesCommand;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.data.LeafDatabase;
import io.github.fisher2911.hmcleaves.debug.Debugger;
import io.github.fisher2911.hmcleaves.hook.Hooks;
import io.github.fisher2911.hmcleaves.listener.InteractionListener;
import io.github.fisher2911.hmcleaves.listener.LeafAndLogEditListener;
import io.github.fisher2911.hmcleaves.listener.LeafDropListener;
import io.github.fisher2911.hmcleaves.listener.PlayerJoinListener;
import io.github.fisher2911.hmcleaves.listener.SoundListener;
import io.github.fisher2911.hmcleaves.listener.WorldAndChunkLoadListener;
import io.github.fisher2911.hmcleaves.nms.FeatureHandler;
import io.github.fisher2911.hmcleaves.packet.BlockBreakManager;
import io.github.fisher2911.hmcleaves.packet.LeavesPacketListener;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class HMCLeaves extends JavaPlugin {

    private LeavesConfig leavesConfig;
    private BlockCache blockCache;
    private LeafDatabase leafDatabase;
    private BlockBreakManager blockBreakManager;
    private WorldAndChunkLoadListener worldAndChunkLoadListener;
    private LeavesPacketListener leavesPacketListener;
    private FeatureHandler featureHandler;

    @Override
    public void onLoad() {
        this.leavesConfig = new LeavesConfig(
                this,
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        );
        this.blockCache = new BlockCache(new ConcurrentHashMap<>());
        try {
            final String version = Bukkit.getServer().getVersion();
            if (version.contains("1.18.2")) {
                this.createFeatureHandler("v1_18_2");
            } else if (version.contains("1.18")) {
                this.createFeatureHandler("v1_18");
            }
            if (version.contains("1.19.4")) {
                this.createFeatureHandler("v1_19_4");
            } else if (version.contains("1.19.3")) {
                this.createFeatureHandler("v1_19_3");
            } else if (version.contains("1.19")) {
                this.createFeatureHandler("v1_19");
            }
            if (version.contains("1.20")) {
                this.createFeatureHandler("v1_20");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createFeatureHandler(String packageVersion) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException {
        this.featureHandler = (FeatureHandler) Class.forName("io.github.fisher2911." + packageVersion + ".FeatureHandler").getConstructors()[0]
                .newInstance(this.leavesConfig, this.blockCache);
        this.featureHandler.register();
    }

    @Override
    public void onEnable() {
        final PacketEventsAPI<Plugin> api = SpigotPacketEventsBuilder.build(this);
        api.getSettings().checkForUpdates(false);
        PacketEvents.setAPI(api);
        PacketEvents.getAPI().load();
        PacketEvents.getAPI().init();
        this.leafDatabase = new LeafDatabase(this);
        this.blockBreakManager = new BlockBreakManager(new ConcurrentHashMap<>(), this);
        this.worldAndChunkLoadListener = new WorldAndChunkLoadListener(this);
        this.leavesPacketListener = new LeavesPacketListener(this);
        this.registerPacketListeners();
        this.registerListeners();
        Hooks.load(this);
        this.leavesConfig.load();
        this.leafDatabase.load();
        Bukkit.getScheduler().runTaskLater(this, () -> this.worldAndChunkLoadListener.loadDefaultWorlds(), 20);
        this.getCommand("hmcleaves").setExecutor(new LeavesCommand(this));
        final int bStatsPluginId = 16900;
        final Metrics metrics = new Metrics(this, bStatsPluginId);
    }

    public void reload() {
        this.leavesConfig.reload();
    }

    @Override
    public void onDisable() {
        this.leafDatabase.shutdownNow().forEach(Runnable::run);
        for (var entry : this.blockCache.getCache().entrySet()) {
            final WorldBlockCache worldBlockCache = entry.getValue();
            for (var chunkEntry : worldBlockCache.getBlockCacheMap().entrySet()) {
                final ChunkBlockCache chunkBlockCache = chunkEntry.getValue();
                if (chunkBlockCache.isClean()) continue;
                this.leafDatabase.saveBlocksInChunk(chunkBlockCache);
            }
        }
        Debugger.getInstance().shutdown();
    }

    private void registerPacketListeners() {
        PacketEvents.getAPI().getEventManager().registerListener(this.leavesPacketListener);
    }

    private void registerListeners() {
        List.of(
                        new InteractionListener(this),
                        new LeafDropListener(this),
                        this.worldAndChunkLoadListener,
                        new LeafAndLogEditListener(this),
                        new SoundListener(this),
                        new PlayerJoinListener(this)
                )
                .forEach(listener -> getServer().getPluginManager().registerEvents(listener, this));
    }

    public LeavesConfig getLeavesConfig() {
        return this.leavesConfig;
    }

    public BlockCache getBlockCache() {
        return this.blockCache;
    }

    public LeafDatabase getLeafDatabase() {
        return this.leafDatabase;
    }

    public BlockBreakManager getBlockBreakManager() {
        return this.blockBreakManager;
    }

    public LeavesPacketListener getLeavesPacketListener() {
        return this.leavesPacketListener;
    }

    public boolean debug() {
        return true;
    }

}
