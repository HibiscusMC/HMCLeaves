package io.github.fisher2911.hmcleaves;

import io.github.fisher2911.hmcleaves.command.LeavesCommand;
import io.github.fisher2911.hmcleaves.hook.Hooks;
import io.github.fisher2911.hmcleaves.listener.LeafDropListener;
import io.github.fisher2911.hmcleaves.listener.PlaceListener;
import io.github.fisher2911.hmcleaves.nms.LeafHandler;
import io.github.fisher2911.hmcleaves.nms.LeafHandler_1_19;
import io.github.fisher2911.hmcleaves.util.PDCHelper;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class HMCLeaves extends JavaPlugin implements Listener {

    private Config config;
    private static final LeafCache leafCache;
    private static final PDCHelper pdcHelper;
    private static final LeafHandler leafHandler;

    static {

        leafCache = new LeafCache(() -> (HMCLeaves) Bukkit.getPluginManager().getPlugin("HMCLeaves"), new ConcurrentHashMap<>());
        pdcHelper = new io.github.fisher2911.hmcleaves.util.PDCHelper(() -> (HMCLeaves) Bukkit.getPluginManager().getPlugin("HMCLeaves"));
        leafHandler = new LeafHandler_1_19(() -> (HMCLeaves) Bukkit.getPluginManager().getPlugin("HMCLeaves"), leafCache, pdcHelper);
        leafHandler.load();
    }

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
        this.config = new Config(this, new HashMap<>());
        this.config.load();
        if (!this.config.isEnabled()) {
            this.getLogger().severe("HMCLeaves is disabled in config.yml");
            return;
        }
        this.getServer().getPluginManager().registerEvents(this, this);
        this.registerListeners();
        this.getCommand("hmcleaves").setExecutor(new LeavesCommand(this));
        Hooks.load(this);
    }

    private void registerListeners() {
        List.of(
                        leafHandler,
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
        return leafCache;
    }

}
