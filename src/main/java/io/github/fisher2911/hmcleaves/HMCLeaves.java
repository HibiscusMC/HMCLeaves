package io.github.fisher2911.hmcleaves;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import io.github.fisher2911.hmcleaves.command.LeavesCommand;
import io.github.fisher2911.hmcleaves.hook.Hooks;
import io.github.fisher2911.hmcleaves.listener.ChunkListener;
import io.github.fisher2911.hmcleaves.listener.LeafDropListener;
import io.github.fisher2911.hmcleaves.listener.PlaceListener;
import io.github.fisher2911.hmcleaves.packet.BlockListener;
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
            this.getLogger().severe("HMCLeaves is disabled in config.yml");
            return;
        }
        this.leafCache = new LeafCache(new ConcurrentHashMap<>());
        PacketEvents.getAPI().init();
        this.getServer().getPluginManager().registerEvents(this, this);
        this.registerListeners();
        new BlockListener(this, this.leafCache).register();
        this.getCommand("hmcleaves").setExecutor(new LeavesCommand(this));
        Hooks.load(this);
    }

    private void registerListeners() {
        List.of(
                        new ChunkListener(this),
                        new PlaceListener(this),
                        new LeafDropListener(this)
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
