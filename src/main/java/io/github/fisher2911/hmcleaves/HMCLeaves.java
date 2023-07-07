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
import io.github.fisher2911.hmcleaves.listener.LeafDropListener;
import io.github.fisher2911.hmcleaves.listener.SoundListener;
import io.github.fisher2911.hmcleaves.listener.WorldAndChunkLoadListener;
import io.github.fisher2911.hmcleaves.packet.BlockBreakManager;
import io.github.fisher2911.hmcleaves.listener.LeafAndLogEditListener;
import io.github.fisher2911.hmcleaves.packet.LeavesPacketListener;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class HMCLeaves extends JavaPlugin {

    private LeavesConfig leavesConfig;
    private BlockCache blockCache;
    private LeafDatabase leafDatabase;
    private BlockBreakManager blockBreakManager;
    private WorldAndChunkLoadListener worldAndChunkLoadListener;

    @Override
    public void onLoad() {
        final PacketEventsAPI<Plugin> api = SpigotPacketEventsBuilder.build(this);
        api.getSettings().checkForUpdates(false);
        PacketEvents.setAPI(api);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        this.leavesConfig = new LeavesConfig(
                this,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        );
        this.blockCache = new BlockCache(new ConcurrentHashMap<>());
        this.leafDatabase = new LeafDatabase(this);
        this.blockBreakManager = new BlockBreakManager(new ConcurrentHashMap<>(), this);
        this.worldAndChunkLoadListener = new WorldAndChunkLoadListener(this);
        PacketEvents.getAPI().init();
        this.registerPacketListeners();
        this.registerListeners();
        Hooks.load(this);
        this.leavesConfig.load();
        this.leafDatabase.load();
        this.worldAndChunkLoadListener.loadDefaultWorlds();
        this.getCommand("hmcleaves").setExecutor(new LeavesCommand(this));
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
        PacketEvents.getAPI().getEventManager().registerListener(new LeavesPacketListener(this));
    }

    private void registerListeners() {
        List.of(
                        new InteractionListener(this),
                        new LeafDropListener(this),
                        this.worldAndChunkLoadListener,
                        new LeafAndLogEditListener(this),
                        new SoundListener(this)
                )
                .forEach(listener -> getServer().getPluginManager().registerEvents(listener, this));
    }

    public LeavesConfig getLeavesConfig() {
        return leavesConfig;
    }

    public BlockCache getBlockCache() {
        return blockCache;
    }

    public LeafDatabase getLeafDatabase() {
        return leafDatabase;
    }

    public BlockBreakManager getBlockBreakManager() {
        return blockBreakManager;
    }

    public boolean debug() {
        return true;
    }

}
