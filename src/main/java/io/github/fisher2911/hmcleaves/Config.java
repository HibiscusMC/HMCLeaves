package io.github.fisher2911.hmcleaves;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class Config {

    private final HMCLeaves plugin;
    private final Map<String, LeafItem> leafItems;
    private int defaultDistance;
    private boolean defaultPersistent;
    private boolean enabled;

    public static final String DEBUG_TOOL_ID = "debugtool";

    public Config(HMCLeaves plugin, Map<String, LeafItem> leafItems) {
        this.plugin = plugin;
        this.leafItems = leafItems;
    }

    public WrappedBlockState getDefaultState(Material material) {
        final WrappedBlockState state = WrappedBlockState.getDefaultState(
                PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                StateTypes.getByName(material.toString().toLowerCase())
        );
        state.setDistance(this.defaultDistance);
        state.setPersistent(this.defaultPersistent);
        return state;
    }

    public void setDefaultState(WrappedBlockState state) {
        state.setDistance(this.defaultDistance);
        state.setPersistent(this.defaultPersistent);
    }

    @Nullable
    public LeafItem getItem(String id) {
        return leafItems.get(id);
    }

    @Nullable
    public LeafItem getByState(WrappedBlockState state) {
        for (LeafItem item : this.leafItems.values()) {
            if (item.leafData().persistent() == state.isPersistent() && item.leafData().distance() == state.getDistance()) {
                return item;
            }
        }
        return null;
    }

    public Map<String, LeafItem> getLeafItems() {
        return leafItems;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void reload() {
        this.leafItems.clear();
        this.plugin.reloadConfig();
        this.load();
    }

    private static final String DEFAULT_STATE_KEY = "default-state";
    private static final String DISTANCE_KEY = "distance";
    private static final String PERSISTENT_KEY = "persistent";
    private static final String ITEMS_KEY = "items";
    private static final String MATERIAL = "material";
    private static final String MODEL_DATA = "model-data";
    private static final String LEAF_MATERIAL = "leaf-material";

    public void load() {
        this.plugin.saveDefaultConfig();
        final FileConfiguration config = this.plugin.getConfig();
        this.enabled = config.getBoolean("enabled", false);
        this.defaultDistance = config.getInt(DEFAULT_STATE_KEY + "." + DISTANCE_KEY, 3);
        this.defaultPersistent = config.getBoolean(DEFAULT_STATE_KEY + "." + PERSISTENT_KEY, false);
        final ConfigurationSection itemSection = config.getConfigurationSection(ITEMS_KEY);
        if (itemSection == null) return;
        for (String id : itemSection.getKeys(false)) {
            final Material material = Material.getMaterial(itemSection.getString(id + "." + MATERIAL));
            final Material leafMaterial = Material.getMaterial(itemSection.getString(id + "." + LEAF_MATERIAL));
            final int modelData = itemSection.getInt(id + "." + MODEL_DATA, -1);
            final int distance = itemSection.getInt(id + "." + DISTANCE_KEY, this.defaultDistance);
            final boolean persistent = itemSection.getBoolean(id + "." + PERSISTENT_KEY, this.defaultPersistent);
            final ItemStack itemStack = new ItemStack(material);
            final ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta != null) {
                itemMeta.setDisplayName(id);
                if (modelData != -1) itemMeta.setCustomModelData(modelData);
                itemMeta.getPersistentDataContainer().set(PDCUtil.ITEM_KEY, PersistentDataType.STRING, id);
                itemStack.setItemMeta(itemMeta);
            }
            this.leafItems.put(id, new LeafItem(id, itemStack, leafMaterial, distance, persistent));
        }

    }
}
