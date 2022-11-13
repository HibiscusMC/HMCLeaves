package io.github.fisher2911.hmcleaves;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import io.github.fisher2911.hmcleaves.hook.Hooks;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Config {

    private final HMCLeaves plugin;
    private final Map<String, LeafItem> leafItems;
    // I'm lazy and don't want to have to check ItemsAdder loading, maybe I'll change it later
    private final Map<String, Supplier<ItemStack>> saplings;
    private final Map<String, Supplier<ItemStack>> leafDropReplacements;
    private int defaultDistance;
    private boolean defaultPersistent;
    private boolean enabled;

    public static final String DEBUG_TOOL_ID = "debugtool";

    public Config(HMCLeaves plugin, Map<String, LeafItem> leafItems) {
        this.plugin = plugin;
        this.leafItems = leafItems;
        this.saplings = new HashMap<>();
        this.leafDropReplacements = new HashMap<>();
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

    @Nullable
    public ItemStack getSapling(String id) {
        final Supplier<ItemStack> supplier = this.saplings.get(id);
        if (supplier == null) return null;
        return supplier.get();
    }

    @Nullable
    public ItemStack getLeafDropReplacement(String id) {
        final Supplier<ItemStack> supplier = this.leafDropReplacements.get(id);
        if (supplier == null) return null;
        return supplier.get();
    }

    public void reload() {
        this.leafItems.clear();
        this.saplings.clear();
        this.leafDropReplacements.clear();
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

    private static final String LEAF_DROP_REPLACEMENTS = "leaf-drop-replacement";
    private static final String SAPLING = "sapling";
    private static final String HOOK_ID = "hook-id";
    private static final String AMOUNT = "amount";
    private static final String NAME = "name";
    private static final String LORE = "lore";

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
            final ConfigurationSection saplingSection = itemSection.getConfigurationSection(id + "." + SAPLING);
            if (saplingSection != null) {
                this.saplings.put(id, this.loadItemStack(saplingSection));
            }
            final ConfigurationSection leafDropReplacementSection = itemSection.getConfigurationSection(id + "." + LEAF_DROP_REPLACEMENTS);
            if (leafDropReplacementSection != null) {
                this.leafDropReplacements.put(id, this.loadItemStack(leafDropReplacementSection));
            }
        }
    }

    private Supplier<ItemStack> loadItemStack(ConfigurationSection section) {
        final String itemId = section.getString(HOOK_ID, null);
        if (itemId != null) return () -> {
            final ItemStack itemStack = Hooks.getItem(itemId);
            if (itemStack == null) return null;
            return itemStack.clone();
        };
        final Material material = Material.getMaterial(section.getString(MATERIAL));
        final int amount = section.getInt(AMOUNT, 1);
        final String name = section.getString(NAME);
        final List<String> lore = section.getStringList(LORE);
        final int modelData = section.getInt(MODEL_DATA, -1);
        final ItemStack itemStack = new ItemStack(material, amount);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            if (name != null) itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (section.contains(MODEL_DATA)) itemMeta.setCustomModelData(modelData);
            if (!lore.isEmpty()) itemMeta.setLore(lore.stream().map(s -> ChatColor.translateAlternateColorCodes('&', s)).collect(Collectors.toList()));
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack::clone;
    }

}
