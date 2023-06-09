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

package io.github.fisher2911.hmcleaves.config;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.enums.Instrument;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.hook.Hooks;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class LeavesConfig {

    public static final String DEBUG_TOOL_ID = "leaves_debug_tool";

    private static final String DEFAULT_LEAF_ID = "default_leaf_id";
    private static final String DEFAULT_LOG_ID = "default_log_id";
    private static final String DEFAULT_STRIPPED_LOG_ID = "default_stripped_log_id";

    private static final int STATES_PER_LEAF = 7 * 2;
    private static final List<Material> LEAVES = new ArrayList<>();
    private static final List<Material> LOGS = new ArrayList<>();
    private static final List<Material> STRIPPED_LOGS = new ArrayList<>();

    private static WrappedBlockState getLeafById(int id) {
        if (id < 0) {
            throw new IllegalStateException("Leaf id must be 0 or greater!");
        }
        final int materialId = id / STATES_PER_LEAF;
        final int distance = id % 7 + 1;
        final boolean persistent = id % STATES_PER_LEAF > STATES_PER_LEAF / 2;
        final Material material = LEAVES.get(materialId);
        final WrappedBlockState state = WrappedBlockState.getDefaultState(
                PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                SpigotConversionUtil.fromBukkitBlockData(material.createBlockData()).getType()
        ).clone();
        state.setDistance(distance);
        state.setPersistent(persistent);
        return state;
    }

    private static WrappedBlockState getLogById(int id) {
        if (id < 0) {
            throw new IllegalStateException("Log id must be 0 or greater!");
        }
        final Material log;
        if (id >= LOGS.size()) {
            log = STRIPPED_LOGS.get(id - LOGS.size());
        } else {
            log = LOGS.get(id);
        }
        return WrappedBlockState.getDefaultState(
                PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                SpigotConversionUtil.fromBukkitBlockData(log.createBlockData()).getType()
        ).clone();
    }

    private static WrappedBlockState getStrippedLogByLogId(int id) {
        return getLogById(id + LOGS.size());
    }

    private static void initLeavesAndLogs(
            Material defaultLeafMaterial,
            Material defaultLogMaterial,
            Material defaultStrippedLogMaterial
    ) {
        LEAVES.add(Material.OAK_LEAVES);
        LEAVES.add(Material.SPRUCE_LEAVES);
        LEAVES.add(Material.BIRCH_LEAVES);
        LEAVES.add(Material.JUNGLE_LEAVES);
        LEAVES.add(Material.ACACIA_LEAVES);
        LEAVES.add(Material.DARK_OAK_LEAVES);

        LOGS.add(Material.OAK_LOG);
        LOGS.add(Material.SPRUCE_LOG);
        LOGS.add(Material.BIRCH_LOG);
        LOGS.add(Material.JUNGLE_LOG);
        LOGS.add(Material.ACACIA_LOG);
        LOGS.add(Material.DARK_OAK_LOG);
        LOGS.add(Material.CRIMSON_STEM);

        STRIPPED_LOGS.add(Material.STRIPPED_OAK_LOG);
        STRIPPED_LOGS.add(Material.STRIPPED_SPRUCE_LOG);
        STRIPPED_LOGS.add(Material.STRIPPED_BIRCH_LOG);
        STRIPPED_LOGS.add(Material.STRIPPED_JUNGLE_LOG);
        STRIPPED_LOGS.add(Material.STRIPPED_ACACIA_LOG);
        STRIPPED_LOGS.add(Material.STRIPPED_DARK_OAK_LOG);
        STRIPPED_LOGS.add(Material.STRIPPED_CRIMSON_STEM);
        // 1.19 leaves / logs
        try {
            LEAVES.add(Material.valueOf("MANGROVE_LEAVES"));
            LEAVES.add(Material.valueOf("AZALEA_LEAVES"));
            LEAVES.add(Material.valueOf("FLOWERING_AZALEA_LEAVES"));

            LOGS.add(Material.valueOf("MANGROVE_LOG"));
        } catch (IllegalArgumentException ignored) {

        }

        LEAVES.remove(defaultLeafMaterial);
        LEAVES.add(defaultLeafMaterial);
        LOGS.remove(defaultLogMaterial);
        LOGS.add(defaultLogMaterial);
        STRIPPED_LOGS.remove(defaultStrippedLogMaterial);
        STRIPPED_LOGS.add(defaultStrippedLogMaterial);
    }

    public static int getMaxLeafId() {
        return LEAVES.size() * STATES_PER_LEAF - 1;
    }

    public static int getMaxLogId() {
        return LOGS.size() - 1;
    }

    public static int getMaxStrippedLogId() {
        return STRIPPED_LOGS.size() - 1;
    }

    private final HMCLeaves plugin;
    private final Map<String, BlockData> blockDataMap;
    private final Map<String, Supplier<ItemStack>> itemSupplierMap;
    private final Map<String, Supplier<ItemStack>> saplingItemSupplierMap;
    private final Map<String, Supplier<ItemStack>> leafDropItemSupplierMap;
    private Material defaultLeafMaterial = Material.OAK_LEAVES;
    private Material defaultLogMaterial = Material.OAK_LOG;
    private Material defaultStrippedLogMaterial = Material.STRIPPED_OAK_LOG;

    public LeavesConfig(
            HMCLeaves plugin,
            Map<String, BlockData> blockDataMap,
            Map<String, Supplier<ItemStack>> itemSupplierMap,
            Map<String, Supplier<ItemStack>> saplingItemSupplierMap,
            Map<String, Supplier<ItemStack>> leafDropItemSupplierMap
    ) {
        this.plugin = plugin;
        this.blockDataMap = blockDataMap;
        this.itemSupplierMap = itemSupplierMap;
        this.saplingItemSupplierMap = saplingItemSupplierMap;
        this.leafDropItemSupplierMap = leafDropItemSupplierMap;
    }

    @Nullable
    public BlockData getBlockData(String id) {
        return this.blockDataMap.get(id);
    }

    public BlockData getBlockData(ItemStack itemStack) {
        final String itemId = PDCUtil.getItemId(itemStack);
        if (itemId == null) return null;
        return this.blockDataMap.get(itemId);
    }

    public BlockData getDefaultLeafData() {
        return this.blockDataMap.get(DEFAULT_LEAF_ID);
    }

    public BlockData getDefaultLogData() {
        return this.blockDataMap.get(DEFAULT_LOG_ID);
    }

    public BlockData getDefaultStrippedLogData() {
        return this.blockDataMap.get(DEFAULT_STRIPPED_LOG_ID);
    }

    @Nullable
    public Supplier<ItemStack> getItem(String id) {
        return this.itemSupplierMap.get(id);
    }

    @Unmodifiable
    public Map<String, Supplier<ItemStack>> getItems() {
        return Collections.unmodifiableMap(this.itemSupplierMap);
    }

    @Nullable
    public Supplier<ItemStack> getSapling(String id) {
        return this.saplingItemSupplierMap.get(id);
    }

    @Nullable
    public Supplier<ItemStack> getLeafDropReplacement(String id) {
        return this.leafDropItemSupplierMap.get(id);
    }

    private static final String DEFAULT_LEAF_MATERIAL_PATH = "default-leaf-material";
    private static final String DEFAULT_LOG_MATERIAL_PATH = "default-log-material";
    private static final String DEFAULT_STRIPPED_LOG_MATERIAL_PATH = "default-stripped-log-material";

    private static final String LEAVES_PATH = "leaves";
    private static final String LOGS_PATH = "logs";

    private static final String LEAF_MATERIAL_PATH = "leaf-material";
    private static final String LOG_MATERIAL_PATH = "log-material";
    private static final String STRIPPED_LOG_MATERIAL_PATH = "stripped-log-material";
    private static final String STATE_ID_PATH = "state-id";

    private static final String SAPLING_PATH = "sapling";
    private static final String LEAF_DROP_REPLACEMENT_PATH = "leaf-drop-replacement";

    public void load() {
        this.plugin.saveDefaultConfig();
        final FileConfiguration config = this.plugin.getConfig();
        try {
            this.defaultLeafMaterial = Material.valueOf(config.getString(DEFAULT_LEAF_MATERIAL_PATH));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.defaultLogMaterial = Material.valueOf(config.getString(DEFAULT_LOG_MATERIAL_PATH));
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.defaultStrippedLogMaterial = Material.valueOf(config.getString(DEFAULT_STRIPPED_LOG_MATERIAL_PATH));
        } catch (IllegalArgumentException ignored) {
        }
        initLeavesAndLogs(this.defaultLeafMaterial, this.defaultLogMaterial, this.defaultStrippedLogMaterial);
        this.loadLeavesSection(config);
        this.loadLogsSection(config);
    }

    public void reload() {
        LEAVES.clear();
        LOGS.clear();
        STRIPPED_LOGS.clear();
        this.itemSupplierMap.clear();
        this.saplingItemSupplierMap.clear();
        this.leafDropItemSupplierMap.clear();
        this.load();
    }

    private void loadLeavesSection(FileConfiguration config) {
        final ConfigurationSection leavesSection = config.getConfigurationSection(LEAVES_PATH);
        if (leavesSection == null) return;
        for (var itemId : leavesSection.getKeys(false)) {
            final Supplier<ItemStack> itemStackSupplier = this.loadItemStack(leavesSection.getConfigurationSection(itemId), itemId);
            this.itemSupplierMap.put(itemId, itemStackSupplier);
            final int stateId = leavesSection.getInt(itemId + "." + STATE_ID_PATH);
            final WrappedBlockState state = getLeafById(stateId);
            final Material leafMaterial = this.loadMaterial(leavesSection, itemId + "." + LEAF_MATERIAL_PATH, this.defaultLeafMaterial);
            final BlockData blockData = BlockData.leafData(
                    itemId,
                    state.getGlobalId(),
                    leafMaterial,
                    state.getDistance(),
                    state.isPersistent()
            );
            this.blockDataMap.put(itemId, blockData);
            this.loadSapling(leavesSection, itemId);
            this.loadLeafDropReplacement(leavesSection, itemId);
        }
        this.blockDataMap.put(DEFAULT_LEAF_ID, BlockData.leafData(
                DEFAULT_LEAF_ID,
                getLeafById(getMaxLeafId()).getGlobalId(),
                this.defaultLeafMaterial,
                getLeafById(getMaxLeafId()).getDistance(),
                getLeafById(getMaxLeafId()).isPersistent()
        ));
    }

    private void loadSapling(ConfigurationSection config, String itemId) {
        final ConfigurationSection saplingSection = config.getConfigurationSection(SAPLING_PATH);
        if (saplingSection == null) return;
        final Supplier<ItemStack> itemStackSupplier = this.loadItemStack(saplingSection, itemId);
        this.saplingItemSupplierMap.put(itemId, itemStackSupplier);
    }

    private void loadLeafDropReplacement(ConfigurationSection config, String itemId) {
        final ConfigurationSection leafDropReplacementSection = config.getConfigurationSection(LEAF_DROP_REPLACEMENT_PATH);
        if (leafDropReplacementSection == null) return;
        final Supplier<ItemStack> itemStackSupplier = this.loadItemStack(leafDropReplacementSection, itemId);
        this.leafDropItemSupplierMap.put(itemId, itemStackSupplier);
    }

    private static final String INSTRUMENT_PATH = "instrument";
    private static final String NOTE_PATH = "note";
    private static final String STRIPPED_LOG_ID_PATH = "stripped-log-id";

    private void loadLogsSection(FileConfiguration config) {
        final ConfigurationSection logsSection = config.getConfigurationSection(LOGS_PATH);
        if (logsSection == null) return;
        for (final var itemId : logsSection.getKeys(false)) {
            final Supplier<ItemStack> itemStackSupplier = this.loadItemStack(logsSection.getConfigurationSection(itemId), itemId);
            this.itemSupplierMap.put(itemId, itemStackSupplier);
            final int stateId = logsSection.getInt(itemId + "." + STATE_ID_PATH) - 1;
            final String strippedLogId = logsSection.getString(itemId + "." + STRIPPED_LOG_ID_PATH);
//            final WrappedBlockState logState = getLogById(stateId);
            final WrappedBlockState state = WrappedBlockState.getDefaultState(StateTypes.NOTE_BLOCK);
            final WrappedBlockState strippedLogState = getStrippedLogByLogId(stateId);
            final Material logMaterial = this.loadMaterial(logsSection, itemId + "." + LOG_MATERIAL_PATH, this.defaultLogMaterial);
            final Material strippedLogMaterial = this.loadMaterial(logsSection, itemId + "." + STRIPPED_LOG_MATERIAL_PATH, this.defaultStrippedLogMaterial);
            try {
                final Instrument instrument = Instrument.valueOf(logsSection.getString(itemId + "." + INSTRUMENT_PATH));
                final int note = logsSection.getInt(itemId + "." + NOTE_PATH);
                state.setInstrument(instrument);
                state.setNote(note);
                strippedLogState.setInstrument(instrument);
                strippedLogState.setNote(note);
            } catch (IllegalArgumentException e) {
                this.plugin.getLogger().severe("Invalid instrument or note for log " + itemId + " in config.yml");
            }

            final BlockData blockData = BlockData.logData(
                    itemId,
                    strippedLogId,
                    state.getGlobalId(),
                    logMaterial,
                    strippedLogMaterial,
                    false,
                    strippedLogState.getGlobalId()
            );
            this.blockDataMap.put(itemId, blockData);
        }
        this.blockDataMap.put(DEFAULT_LOG_ID, BlockData.logData(
                DEFAULT_LOG_ID,
                DEFAULT_STRIPPED_LOG_ID,
                getLogById(getMaxLogId()).getGlobalId(),
                this.defaultLogMaterial,
                this.defaultStrippedLogMaterial,
                false,
                getLogById(getMaxStrippedLogId()).getGlobalId()
        ));
        this.blockDataMap.put(DEFAULT_STRIPPED_LOG_ID, BlockData.logData(
                DEFAULT_LOG_ID,
                DEFAULT_STRIPPED_LOG_ID,
                getLogById(getMaxLogId()).getGlobalId(),
                this.defaultLogMaterial,
                this.defaultStrippedLogMaterial,
                true,
                getLogById(getMaxStrippedLogId()).getGlobalId()
        ));
    }

    private Material loadMaterial(ConfigurationSection section, String path, Material defaultMaterial) {
        try {
            return Material.valueOf(section.getString(path));
        } catch (IllegalArgumentException e) {
            return defaultMaterial;
        }
    }

    private static final String MATERIAL_PATH = "material";
    private static final String HOOK_ID_PATH = "hook-id";
    private static final String AMOUNT_PATH = "amount";
    private static final String NAME_PATH = "name";
    private static final String LORE_PATH = "lore";
    private static final String MODEL_DATA_PATH = "model-data";

    private Supplier<ItemStack> loadItemStack(ConfigurationSection section, String itemId) {
        final String hookId = section.getString(HOOK_ID_PATH, null);
        if (hookId != null) return () -> {
            final ItemStack itemStack = Hooks.getItem(hookId);
            if (itemStack == null) return null;
            final ItemStack clone = itemStack.clone();
            PDCUtil.setItemId(clone, itemId);
            return clone;
        };
        final String materialStr = section.getString(MATERIAL_PATH);
        final Material material;
        try {
            material = Material.valueOf(materialStr);
        } catch (Exception e) {
            throw new IllegalStateException("Material not found for " + materialStr + " item.");
        }
        final int amount = section.getInt(AMOUNT_PATH, 1);
        final String name = section.getString(NAME_PATH);
        final List<String> lore = section.getStringList(LORE_PATH);
        final int modelData = section.getInt(MODEL_DATA_PATH, -1);
        final ItemStack itemStack = new ItemStack(material, amount);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            if (name != null) itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (section.contains(MODEL_DATA_PATH)) itemMeta.setCustomModelData(modelData);
            if (!lore.isEmpty()) {
                itemMeta.setLore(lore.stream().map(s -> ChatColor.translateAlternateColorCodes('&', s)).collect(Collectors.toList()));
            }
            itemStack.setItemMeta(itemMeta);
            PDCUtil.setItemId(itemStack, itemId);
        }
        return itemStack::clone;
    }

}
