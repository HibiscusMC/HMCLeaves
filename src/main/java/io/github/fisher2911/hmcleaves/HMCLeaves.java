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
import io.github.fisher2911.hmcleaves.command.LeavesCommand;
import io.github.fisher2911.hmcleaves.hook.Hooks;
import io.github.fisher2911.hmcleaves.listener.ChunkListener;
import io.github.fisher2911.hmcleaves.listener.LeafDropListener;
import io.github.fisher2911.hmcleaves.listener.LeafUpdateListener;
import io.github.fisher2911.hmcleaves.listener.PlaceListener;
import io.github.fisher2911.hmcleaves.packet.BlockListener;
import io.github.fisher2911.hmcleaves.util.LeafUpdater;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class HMCLeaves extends JavaPlugin implements Listener {

    private Config config;
    private LeafCache leafCache;
    ;

    @Override
    public void onLoad() {
        final PacketEventsAPI<Plugin> api = SpigotPacketEventsBuilder.build(this);
        api.getSettings().debug(true);
        PacketEvents.setAPI(api);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        this.config = new Config(this, new HashMap<>());
        this.config.load();
        if (!this.config.isEnabled()) {
            this.getLogger().severe("-=-=-=-=-=HMCLeaves=-=-=-=-=-");
            this.getLogger().severe("HMCLeaves is disabled in config.yml");
            this.getLogger().severe("Disabling your server, make sure to set \"enabled\":true in config.yml, along with your default leaf state!");
            this.getLogger().severe("-=-=-=-=-=HMCLeaves=-=-=-=-=-");
            return;
        }
        this.leafCache = new LeafCache(this, new ConcurrentHashMap<>());
        PacketEvents.getAPI().init();
        this.getServer().getPluginManager().registerEvents(this, this);
        this.registerListeners();
        new BlockListener(this, this.leafCache).register();
        this.getCommand("hmcleaves").setExecutor(new LeavesCommand(this));
        Hooks.load(this);
        LeafUpdater.start();
    }

    private void registerListeners() {
        List.of(
                        new ChunkListener(this),
                        new PlaceListener(this),
                        new LeafDropListener(this),
                        new LeafUpdateListener(this)
                ).
                forEach(listener -> this.getServer().getPluginManager().registerEvents(listener, this));
    }

    @Override
    public void onDisable() {
    }

    public void reload() {
        this.config.reload();
    }

    public Config config() {
        return this.config;
    }

    public LeafCache getLeafCache() {
        return this.leafCache;
    }

}
