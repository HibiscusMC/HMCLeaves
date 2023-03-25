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
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import io.github.fisher2911.hmcleaves.hook.Hooks;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import org.bukkit.ChatColor;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Config {

    private static final List<Material> LEAVES = new ArrayList<>();

    static {
        LEAVES.add(Material.OAK_LEAVES);
        LEAVES.add(Material.SPRUCE_LEAVES);
        LEAVES.add(Material.BIRCH_LEAVES);
        LEAVES.add(Material.JUNGLE_LEAVES);
        LEAVES.add(Material.ACACIA_LEAVES);
        LEAVES.add(Material.DARK_OAK_LEAVES);
        // 1.19 leaves
        try {
            LEAVES.add(Material.valueOf("MANGROVE_LEAVES"));
            LEAVES.add(Material.valueOf("AZALEA_LEAVES"));
            LEAVES.add(Material.valueOf("FLOWERING_AZALEA_LEAVES"));
        } catch (IllegalArgumentException ignored) {

        }
    }

    // cannot use states:
    private static final List<Integer> UNUSABLE_STATES = List.of(13, 27, 41, 55, 69, 83, 97, 111, 125);

    // distance * persistent
    private static final int STATES_PER_LEAF = 7 * 2;

    private final HMCLeaves plugin;
    private final Map<String, LeafItem> leafItems;
    // I'm lazy and don't want to have to check ItemsAdder loading, maybe I'll change it later
    private final Map<String, Supplier<ItemStack>> saplings;
    private final Map<String, Supplier<ItemStack>> leafDropReplacements;
    private final Set<NoteBlockState> allowedLogs;
    private static final int DEFAULT_DISTANCE = 7;
    private static final boolean DEFAULT_PERSISTENCE = true;
    private boolean defaultActuallyPersistent;
    private boolean enabled;

    public static final String DEBUG_TOOL_ID = "debugtool";

    public Config(HMCLeaves plugin, Map<String, LeafItem> leafItems) {
        this.plugin = plugin;
        this.leafItems = leafItems;
        this.saplings = new HashMap<>();
        this.leafDropReplacements = new HashMap<>();
        this.allowedLogs = new HashSet<>();
    }

    public FakeLeafState getDefaultState(Material material) {
        final var type = StateTypes.getByName(material.toString().toLowerCase());
        if (type == null) {
            this.plugin.getLogger().severe("Could not find state type for " + material.toString());
        }
        final WrappedBlockState state = WrappedBlockState.getDefaultState(
                PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                StateTypes.getByName(material.toString().toLowerCase())
        ).clone();
        state.setDistance(DEFAULT_DISTANCE);
        state.setPersistent(DEFAULT_PERSISTENCE);
        return new FakeLeafState(state, material, this.defaultActuallyPersistent, this.defaultActuallyPersistent ? 1 : 7);
    }

    public LeafData getDefaultData(Material material) {
        return new LeafData(material, DEFAULT_DISTANCE, DEFAULT_PERSISTENCE, this.defaultActuallyPersistent);
    }

    public boolean isLogBlock(BlockData block) {
        if (block instanceof final NoteBlock noteBlock) {
            return this.allowedLogs.contains(new NoteBlockState(noteBlock.getInstrument(), noteBlock.getNote().getId()));
        }
        return this.isLogBlock(block.getMaterial());
    }

    private boolean isLogBlock(Material material) {
        return Tag.LOGS.isTagged(material);
    }

    @Nullable
    public LeafItem getItem(String id) {
        return leafItems.get(id);
    }

    @Nullable
    public LeafItem getByState(FakeLeafState fakeLeafState) {
        final var state = fakeLeafState.state();
        for (LeafItem item : this.leafItems.values()) {
            if (item.leafData().persistent() == state.isPersistent() && item.leafData().distance() == state.getDistance() && fakeLeafState.actuallyPersistent() == item.leafData().actuallyPersistent()) {
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

    public int getDefaultDistance() {
        return DEFAULT_DISTANCE;
    }

    public boolean isDefaultPersistent() {
        return DEFAULT_PERSISTENCE;
    }

    public boolean isDefaultActuallyPersistent() {
        return defaultActuallyPersistent;
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

    private static final String DISTANCE_KEY = "distance";
    private static final String PERSISTENT_KEY = "persistent";
    private static final String ACTUALLY_PERSISTENT = "actually-persistent";
    private static final String ITEMS_KEY = "items";
    private static final String MATERIAL = "material";
    private static final String MODEL_DATA = "model-data";
    private static final String LEAF_MATERIAL = "leaf-material";
    private static final String STATE_ID = "state-id";

    private static final String LEAF_DROP_REPLACEMENTS = "leaf-drop-replacement";
    private static final String SAPLING = "sapling";
    private static final String HOOK_ID = "hook-id";
    private static final String AMOUNT = "amount";
    private static final String NAME = "name";
    private static final String LORE = "lore";

    private static final String LOGS = "logs";
    private static final String NOTE_BLOCKS = "noteblocks";
    private static final String INSTRUMENT_PATH = "instrument";
    private static final String NOTE_PATH = "note";

    public void load() {
        this.plugin.saveDefaultConfig();
        final FileConfiguration config = this.plugin.getConfig();
        this.enabled = config.getBoolean("enabled", false);

        final ConfigurationSection logSection = config.getConfigurationSection(LOGS + "." + NOTE_BLOCKS);
        if (logSection != null) {
            this.loadLogs(logSection);
        }

        final ConfigurationSection itemSection = config.getConfigurationSection(ITEMS_KEY);
        if (itemSection == null) return;
        for (String configId : itemSection.getKeys(false)) {
            final int stateId = itemSection.getInt(configId + "." + STATE_ID, -1);
            final LeafData leafData;
            final boolean actuallyPersistent = itemSection.getBoolean(configId + "." + ACTUALLY_PERSISTENT, false);
            final Material material = Material.getMaterial(itemSection.getString(configId + "." + MATERIAL));
            final int modelData = itemSection.getInt(configId + "." + MODEL_DATA, -1);
            if (stateId == -1) {
                final Material leafMaterial = Material.getMaterial(itemSection.getString(configId + "." + LEAF_MATERIAL));
                final int distance = itemSection.getInt(configId + "." + DISTANCE_KEY, DEFAULT_DISTANCE);
                final boolean persistent = itemSection.getBoolean(configId + "." + PERSISTENT_KEY, DEFAULT_PERSISTENCE);
                leafData = new LeafData(leafMaterial, distance, persistent, actuallyPersistent);
            } else {
                leafData = this.calculateNextLeafState(actuallyPersistent, stateId);
            }
            final ItemStack itemStack = new ItemStack(material);
            final ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta != null) {
                itemMeta.setDisplayName(configId);
                if (modelData != -1) itemMeta.setCustomModelData(modelData);
                itemMeta.getPersistentDataContainer().set(PDCUtil.ITEM_KEY, PersistentDataType.STRING, configId);
                itemStack.setItemMeta(itemMeta);
            }
//            this.leafItems.put(id, new LeafItem(configId, itemStack, leafMaterial, distance, persistent, actuallyPersistent));
            final LeafItem leafItem = new LeafItem(
                    configId,
                    itemStack,
                    leafData.material(),
                    leafData.distance(),
                    leafData.persistent(),
                    leafData.actuallyPersistent()
            );
            this.leafItems.put(configId, leafItem);
            final ConfigurationSection saplingSection = itemSection.getConfigurationSection(configId + "." + SAPLING);
            if (saplingSection != null) {
                this.saplings.put(configId, this.loadItemStack(saplingSection));
            }
            final ConfigurationSection leafDropReplacementSection = itemSection.getConfigurationSection(configId + "." + LEAF_DROP_REPLACEMENTS);
            if (leafDropReplacementSection != null) {
                this.leafDropReplacements.put(configId, this.loadItemStack(leafDropReplacementSection));
            }
        }
    }

    private void loadLogs(ConfigurationSection section) {
        for (String id : section.getKeys(false)) {
            final byte note = (byte) section.getInt(id + "." + NOTE_PATH, 0);
            final String instrumentStr = section.getString(id + "." + INSTRUMENT_PATH, "");
            final Instrument instrument = Instrument.valueOf(instrumentStr.toUpperCase(Locale.ROOT));
            this.allowedLogs.add(new NoteBlockState(instrument, note));
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
            if (!lore.isEmpty())
                itemMeta.setLore(lore.stream().map(s -> ChatColor.translateAlternateColorCodes('&', s)).collect(Collectors.toList()));
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack::clone;
    }


    private LeafData calculateNextLeafState(boolean actuallyPersistent, int i) {
        final int id = i % STATES_PER_LEAF;
        final Material material = LEAVES.get(i / STATES_PER_LEAF);
        final int distance = id % 7 + 1;
        final boolean persistent = id >= 7;
        final WrappedBlockState state = WrappedBlockState.getDefaultState(
                PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                StateTypes.getByName(material.toString().toLowerCase())
        ).clone();
        state.setDistance(distance);
        state.setPersistent(persistent);
        return new LeafData(material, distance, persistent, actuallyPersistent);
    }

}
