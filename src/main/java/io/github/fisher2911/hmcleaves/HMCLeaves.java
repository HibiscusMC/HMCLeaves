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
import io.github.fisher2911.hmcleaves.command.LeavesCommand;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.hook.Hooks;
import io.github.fisher2911.hmcleaves.listener.InteractionListener;
import io.github.fisher2911.hmcleaves.listener.LeafDropListener;
import io.github.fisher2911.hmcleaves.packet.PacketListener;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class HMCLeaves extends JavaPlugin {

    private LeavesConfig leavesConfig;
    private BlockCache blockCache;

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
                new HashMap<>()
        );
        this.blockCache = new BlockCache(new ConcurrentHashMap<>());
        PacketEvents.getAPI().init();
        this.registerPacketListeners();
        this.registerListeners();
        Hooks.load(this);
        this.leavesConfig.load();
        this.getCommand("hmcleaves").setExecutor(new LeavesCommand(this));
    }

    public void reload() {
        this.leavesConfig.reload();
    }

    @Override
    public void onDisable() {
    }

    private void registerPacketListeners() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListener(this.blockCache));
//        ProtocolLibrary.getProtocolManager().addPacketListener(new BlockPacketListener(this, ListenerPriority.HIGH));
//        ProtocolLibrary.getProtocolManager().addPacketListener(new ChunkPacketListener(this, ListenerPriority.HIGH));
    }

    private void registerListeners() {
        List.of(
                        new InteractionListener(this),
                        new LeafDropListener(this)
                )
                .forEach(listener -> getServer().getPluginManager().registerEvents(listener, this));
    }

    public LeavesConfig getLeavesConfig() {
        return leavesConfig;
    }

    public BlockCache getBlockCache() {
        return blockCache;
    }

}
