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
import io.github.fisher2911.hmcleaves.data.CaveVineData;
import io.github.fisher2911.hmcleaves.data.LogData;
import io.github.fisher2911.hmcleaves.hook.Hooks;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class LeavesConfig {

    public static final String DEBUG_TOOL_ID = "leaves_debug_tool";

    private static final String DEFAULT_LEAF_ID = "default_leaf_id";
    private static final String DEFAULT_LOG_ID = "default_log_id";
    private static final String DEFAULT_STRIPPED_LOG_ID = "default_stripped_log_id";
    private static final String DEFAULT_SAPLING_ID = "default_sapling_id";
    private static final String DEFAULT_CAVE_VINES_ID = "default_cave_vines_id";

    private static final int STATES_PER_LEAF = 7 * 2;
    private static final List<Material> LEAVES = new ArrayList<>();
    private static final List<Material> LOGS = new ArrayList<>();
    private static final List<Material> STRIPPED_LOGS = new ArrayList<>();
    private static final List<Material> SAPLINGS = new ArrayList<>();

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

    private static WrappedBlockState getCaveVinesById(int id) {
        final boolean berries = id > 25;
        final int age = (id - 1) % 25 + 1;
        final WrappedBlockState state = StateTypes.CAVE_VINES.createBlockState();
        state.setBerries(berries);
        state.setAge(age);
        return state;
    }

    private static WrappedBlockState getStrippedLogByLogId(int id) {
        return getLogById(id + LOGS.size());
    }

    private static void initLeavesAndLogs() {
        LEAVES.add(Material.OAK_LEAVES);
        LEAVES.add(Material.SPRUCE_LEAVES);
        LEAVES.add(Material.BIRCH_LEAVES);
        LEAVES.add(Material.JUNGLE_LEAVES);
        LEAVES.add(Material.ACACIA_LEAVES);
        LEAVES.add(Material.DARK_OAK_LEAVES);
        LEAVES.add(Material.AZALEA_LEAVES);
        LEAVES.add(Material.FLOWERING_AZALEA_LEAVES);

        LOGS.add(Material.OAK_LOG);
        LOGS.add(Material.SPRUCE_LOG);
        LOGS.add(Material.BIRCH_LOG);
        LOGS.add(Material.JUNGLE_LOG);
        LOGS.add(Material.ACACIA_LOG);
        LOGS.add(Material.DARK_OAK_LOG);
        LOGS.add(Material.CRIMSON_STEM);
        LOGS.add(Material.WARPED_STEM);
        LOGS.add(Material.OAK_WOOD);
        LOGS.add(Material.SPRUCE_WOOD);
        LOGS.add(Material.BIRCH_WOOD);
        LOGS.add(Material.JUNGLE_WOOD);
        LOGS.add(Material.ACACIA_WOOD);
        LOGS.add(Material.DARK_OAK_WOOD);
        LOGS.add(Material.CRIMSON_HYPHAE);
        LOGS.add(Material.WARPED_HYPHAE);

        STRIPPED_LOGS.add(Material.STRIPPED_OAK_LOG);
        STRIPPED_LOGS.add(Material.STRIPPED_SPRUCE_LOG);
        STRIPPED_LOGS.add(Material.STRIPPED_BIRCH_LOG);
        STRIPPED_LOGS.add(Material.STRIPPED_JUNGLE_LOG);
        STRIPPED_LOGS.add(Material.STRIPPED_ACACIA_LOG);
        STRIPPED_LOGS.add(Material.STRIPPED_DARK_OAK_LOG);
        STRIPPED_LOGS.add(Material.STRIPPED_CRIMSON_STEM);
        STRIPPED_LOGS.add(Material.STRIPPED_WARPED_STEM);
        STRIPPED_LOGS.add(Material.STRIPPED_OAK_WOOD);
        STRIPPED_LOGS.add(Material.STRIPPED_SPRUCE_WOOD);
        STRIPPED_LOGS.add(Material.STRIPPED_BIRCH_WOOD);
        STRIPPED_LOGS.add(Material.STRIPPED_JUNGLE_WOOD);
        STRIPPED_LOGS.add(Material.STRIPPED_ACACIA_WOOD);
        STRIPPED_LOGS.add(Material.STRIPPED_DARK_OAK_WOOD);
        STRIPPED_LOGS.add(Material.STRIPPED_CRIMSON_HYPHAE);
        STRIPPED_LOGS.add(Material.STRIPPED_WARPED_HYPHAE);

        SAPLINGS.add(Material.OAK_SAPLING);
        SAPLINGS.add(Material.SPRUCE_SAPLING);
        SAPLINGS.add(Material.BIRCH_SAPLING);
        SAPLINGS.add(Material.JUNGLE_SAPLING);
        SAPLINGS.add(Material.ACACIA_SAPLING);
        SAPLINGS.add(Material.DARK_OAK_SAPLING);
        // 1.19 leaves / logs
        try {
            LEAVES.add(Material.valueOf("MANGROVE_LEAVES"));

            LOGS.add(Material.valueOf("MANGROVE_LOG"));
            LOGS.add(Material.valueOf("MANGROVE_WOOD"));
            STRIPPED_LOGS.add(Material.valueOf("STRIPPED_MANGROVE_LOG"));
            STRIPPED_LOGS.add(Material.valueOf("STRIPPED_MANGROVE_WOOD"));
        } catch (IllegalArgumentException ignored) {

        }
    }

    public static int getDefaultLeafId(Material leafMaterial) {
        return LEAVES.indexOf(leafMaterial) * STATES_PER_LEAF;
    }

    public static int getDefaultLogId(Material logMaterial) {
        return LOGS.indexOf(logMaterial);
    }

    public static int getDefaultStrippedLogId(Material logMaterial, boolean usingStrippedMaterial) {
        if (usingStrippedMaterial) {
            return STRIPPED_LOGS.indexOf(logMaterial);
        }
        final int index = LOGS.indexOf(logMaterial);
        return index + LOGS.size();
    }

    public static String getDefaultLeafStringId(Material material) {
        return DEFAULT_LEAF_ID + "_" + material.name().toLowerCase();
    }

    public static String getDefaultLogStringId(Material material) {
        return DEFAULT_LOG_ID + "_" + material.name().toLowerCase();
    }

    public static String getDefaultStrippedLogStringId(Material material) {
        return DEFAULT_STRIPPED_LOG_ID + "_" + material.name().toLowerCase();
    }

    public static String getDefaultSaplingStringId(Material material) {
        return DEFAULT_SAPLING_ID + "_" + material.name().toLowerCase();
    }

    public static String getDefaultCaveVinesStringId(boolean glowBerries) {
        return DEFAULT_CAVE_VINES_ID + "_" + (glowBerries ? "glow" : "normal");
    }

    private final HMCLeaves plugin;
    private final TextureFileGenerator textureFileGenerator;
    private final Map<String, BlockData> blockDataMap;
    // so that tab complete doesn't show directional ID's
    private final Set<String> playerItemIds;
    private final Map<String, Supplier<ItemStack>> itemSupplierMap;
    private final Map<String, Supplier<ItemStack>> saplingItemSupplierMap;
    private final Map<String, Supplier<ItemStack>> leafDropItemSupplierMap;
    // what blocks a sapling can be placed on
    private final Map<String, Predicate<Location>> saplingSoilPredicateMap;
    private Material defaultLeafMaterial = Material.OAK_LEAVES;
    private Material defaultLogMaterial = Material.OAK_LOG;
    private Material defaultStrippedLogMaterial = Material.STRIPPED_OAK_LOG;

    private boolean useWorldWhitelist;
    private Set<String> whitelistedWorlds;

    public LeavesConfig(
            HMCLeaves plugin,
            Map<String, BlockData> blockDataMap,
            Map<String, Supplier<ItemStack>> itemSupplierMap,
            Map<String, Supplier<ItemStack>> saplingItemSupplierMap,
            Map<String, Supplier<ItemStack>> leafDropItemSupplierMap,
            Map<String, Predicate<Location>> saplingSoilPredicateMap
    ) {
        this.plugin = plugin;
        this.textureFileGenerator = new TextureFileGenerator(plugin);
        this.playerItemIds = new HashSet<>();
        this.blockDataMap = blockDataMap;
        this.itemSupplierMap = itemSupplierMap;
        this.saplingItemSupplierMap = saplingItemSupplierMap;
        this.leafDropItemSupplierMap = leafDropItemSupplierMap;
        this.saplingSoilPredicateMap = saplingSoilPredicateMap;
    }

    @Nullable
    public BlockData getBlockData(String id) {
        return this.blockDataMap.get(id);
    }

    @Nullable
    public BlockData getBlockData(ItemStack itemStack) {
        String itemId = PDCUtil.getItemId(itemStack);
        if (itemId == null) {
            itemId = Hooks.getItemId(itemStack);
        }
        if (itemId == null) return null;
        return this.blockDataMap.get(itemId);
    }

    @Nullable
    public BlockData getBlockData(ItemStack itemStack, Axis axis) {
        String itemId = PDCUtil.getItemId(itemStack);
        if (itemId == null) {
            itemId = Hooks.getItemId(itemStack);
        }
        if (itemId == null) return null;
        final BlockData data = this.blockDataMap.get(itemId + "_" + axis.name().toLowerCase());
        if (data != null) return data;
        return this.blockDataMap.get(itemId);
    }

    @Nullable
    public BlockData getDefaultLeafData(Material leafMaterial) {
        return this.blockDataMap.get(getDefaultLeafStringId(leafMaterial));
    }

    @Nullable
    public BlockData getDefaultLogData(Material logMaterial) {
        return this.blockDataMap.get(getDefaultLogStringId(logMaterial));
    }

    @Nullable
    public BlockData getDefaultLogData(Material logMaterial, Axis axis) {
        return this.blockDataMap.get(getDefaultLogStringId(logMaterial) + "_" + axis.name().toLowerCase());
    }

    @Nullable
    public BlockData getDefaultStrippedLogData(Material strippedLogMaterial) {
        return this.blockDataMap.get(getDefaultStrippedLogStringId(strippedLogMaterial));
    }

    @Nullable
    public BlockData getDefaultStrippedLogData(Material strippedLogMaterial, Axis axis) {
        return this.blockDataMap.get(getDefaultStrippedLogStringId(strippedLogMaterial) + "_" + axis.name().toLowerCase());
    }

    @Nullable
    public BlockData getDefaultSaplingData(Material material) {
        return this.blockDataMap.get(getDefaultSaplingStringId(material));
    }

    @Nullable
    public BlockData getDefaultCaveVinesData(boolean glowBerries) {
        return this.blockDataMap.get(getDefaultCaveVinesStringId(glowBerries));
    }

    @Nullable
    public Supplier<ItemStack> getItemSupplier(String id) {
        return this.itemSupplierMap.get(id);
    }

    @Nullable
    public ItemStack getItemStack(String id) {
        final Supplier<ItemStack> supplier = this.getItemSupplier(id);
        if (supplier == null) return null;
        return supplier.get();
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

    @Unmodifiable
    public Set<String> getPlayerItemIds() {
        return Collections.unmodifiableSet(playerItemIds);
    }

    private static final String DEFAULT_LEAF_MATERIAL_PATH = "default-leaf-material";
    private static final String DEFAULT_LOG_MATERIAL_PATH = "default-log-material";
    private static final String DEFAULT_STRIPPED_LOG_MATERIAL_PATH = "default-stripped-log-material";

    private static final String LEAVES_PATH = "leaves";
    private static final String LOGS_PATH = "logs";
    private static final String SAPLINGS_PATH = "saplings";
    private static final String CAVE_VINES_PATH = "cave-vines";

    private static final String LEAF_MATERIAL_PATH = "leaf-material";
    private static final String LOG_MATERIAL_PATH = "log-material";
    private static final String WORLD_PERSISTENCE_PATH = "world-persistence";
    private static final String STRIPPED_LOG_MATERIAL_PATH = "stripped-log-material";
    private static final String STATE_ID_PATH = "state-id";
    private static final String SAPLING_MATERIAL_PATH = "sapling-material";

    private static final String SAPLING_PATH = "sapling";
    private static final String LEAF_DROP_REPLACEMENT_PATH = "leaf-drop-replacement";

    private static final String MODEL_PATH_PATH = "model-path";

    private static final String ONLY_FOLLOW_WORLD_PERSISTENCE_IF_CONNECTED_TO_LOG_PATH = "only-follow-world-persistence-if-connected-to-log";
    private boolean onlyFollowWorldPersistenceIfConnectedToLog;

    private static final String USE_WORLD_WHITELIST_PATH = "use-world-whitelist";
    private static final String WHITELISTED_WORLDS_PATH = "whitelisted-worlds";

    public void load() {
        this.plugin.saveDefaultConfig();
        final FileConfiguration config = this.plugin.getConfig();
        this.onlyFollowWorldPersistenceIfConnectedToLog = config.getBoolean(ONLY_FOLLOW_WORLD_PERSISTENCE_IF_CONNECTED_TO_LOG_PATH, false);
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
        this.useWorldWhitelist = config.getBoolean(USE_WORLD_WHITELIST_PATH, false);
        this.whitelistedWorlds = new HashSet<>(config.getStringList(WHITELISTED_WORLDS_PATH));
        initLeavesAndLogs();
        this.loadLeavesSection(config);
        this.loadLogsSection(config);
        this.loadSaplingsSection(config);
        this.loadCaveVinesSection(config);
        for (Material leaf : LEAVES) {
            this.textureFileGenerator.generateFile(
                    leaf,
                    this.blockDataMap.values().stream()
                            .filter(blockData -> blockData.worldBlockType() == leaf)
                            .filter(blockData -> blockData.modelPath() != null)
                            .collect(Collectors.toList())
            );
        }
        for (Material sapling : SAPLINGS) {
            this.textureFileGenerator.generateFile(
                    sapling,
                    this.blockDataMap.values().stream()
                            .filter(blockData -> blockData.worldBlockType() == sapling)
                            .filter(blockData -> blockData.modelPath() != null)
                            .collect(Collectors.toList())
            );
        }
    }

    public boolean isOnlyFollowWorldPersistenceIfConnectedToLog() {
        return this.onlyFollowWorldPersistenceIfConnectedToLog;
    }

    public boolean isWorldWhitelisted(String world) {
        return !this.useWorldWhitelist || this.whitelistedWorlds.contains(world);
    }

    public boolean isWorldWhitelisted(World world) {
        return this.isWorldWhitelisted(world.getName());
    }

    public void addWhitelistedWorld(String world) {
        this.whitelistedWorlds.add(world);
    }

    public void addWhitelistedWorld(World world) {
        this.addWhitelistedWorld(world.getName());
    }

    public void removeWhitelistedWorld(String world) {
        this.whitelistedWorlds.remove(world);
    }

    public void removeWhitelistedWorld(World world) {
        this.removeWhitelistedWorld(world.getName());
    }

    public boolean isUseWorldWhitelist() {
        return useWorldWhitelist;
    }

    public boolean canPlaceSaplingOn(String saplingId, Location location) {
        final Predicate<Location> soilPredicate = this.saplingSoilPredicateMap.get(saplingId);
        return soilPredicate != null && soilPredicate.test(location);
    }

    public void reload() {
        LEAVES.clear();
        LOGS.clear();
        STRIPPED_LOGS.clear();
        SAPLINGS.clear();
        this.itemSupplierMap.clear();
        this.saplingItemSupplierMap.clear();
        this.leafDropItemSupplierMap.clear();
        this.playerItemIds.clear();
        this.plugin.reloadConfig();
        this.load();
    }

    private void loadLeavesSection(FileConfiguration config) {
        final ConfigurationSection leavesSection = config.getConfigurationSection(LEAVES_PATH);
        if (leavesSection == null) return;
        for (var itemId : leavesSection.getKeys(false)) {
            final Supplier<ItemStack> itemStackSupplier = this.loadItemStack(
                    leavesSection.getConfigurationSection(itemId),
                    itemId,
                    itemId
//                    HOOK_ID_PATH
            );
            this.itemSupplierMap.put(itemId, itemStackSupplier);
            this.playerItemIds.add(itemId);
            final int stateId = leavesSection.getInt(itemId + "." + STATE_ID_PATH);
            final WrappedBlockState state = getLeafById(stateId);
            final Material leafMaterial = this.loadMaterial(leavesSection, itemId + "." + LEAF_MATERIAL_PATH, this.defaultLeafMaterial);
            final boolean worldPersistence = leavesSection.getBoolean(itemId + "." + WORLD_PERSISTENCE_PATH, state.isPersistent());
            final String modelPath = leavesSection.getString(itemId + "." + MODEL_PATH_PATH, null);
            final BlockData blockData = BlockData.leafData(
                    itemId,
                    state.getGlobalId(),
                    leafMaterial,
                    state.getDistance(),
                    state.isPersistent(),
                    worldPersistence,
                    false,
                    modelPath
            );
            this.blockDataMap.put(itemId, blockData);
            this.loadSapling(leavesSection.getConfigurationSection(itemId), itemId);
            this.loadLeafDropReplacement(leavesSection.getConfigurationSection(itemId), itemId);
        }
        for (Material leaf : LEAVES) {
            final String defaultLeafStringId = getDefaultLeafStringId(leaf);
            final int defaultLeafId = getDefaultLeafId(leaf);
            final WrappedBlockState leafStateById = getLeafById(defaultLeafId);
            this.blockDataMap.put(defaultLeafStringId, BlockData.leafData(
                    defaultLeafStringId,
                    leafStateById.getGlobalId(),
                    leaf,
                    leafStateById.getDistance(),
                    leafStateById.isPersistent(),
                    false,
                    false,
                    null
            ));
        }
    }

    private void loadSapling(ConfigurationSection config, String itemId) {
        this.loadDropReplacement(config, itemId, SAPLING_PATH, this.saplingItemSupplierMap);
    }

    private void loadLeafDropReplacement(ConfigurationSection config, String itemId) {
        this.loadDropReplacement(config, itemId, LEAF_DROP_REPLACEMENT_PATH, this.leafDropItemSupplierMap);
    }

    private void loadDropReplacement(
            ConfigurationSection config,
            String itemId,
            String itemPath,
            Map<String, Supplier<ItemStack>> supplierMap
    ) {
        final String replacementId = config.getString(itemPath);
        final Supplier<ItemStack> itemStackSupplier = () -> {
            final Supplier<ItemStack> supplier = this.itemSupplierMap.get(replacementId);
            if (supplier == null) return null;
            final ItemStack itemStack = supplier.get();
            if (itemStack == null) return null;
            return supplier.get();
        };
        supplierMap.put(itemId, itemStackSupplier);
    }

    private static final String INSTRUMENT_PATH = "instrument";
    private static final String STRIPPED_INSTRUMENT_PATH = "stripped-instrument";
    private static final String NOTE_PATH = "note";
    private static final String STRIPPED_NOTE_PATH = "stripped-note";
    private static final String STRIPPED_LOG_ID_PATH = "stripped-log-id";

    private void loadLogsSection(FileConfiguration config) {
        final ConfigurationSection logsSection = config.getConfigurationSection(LOGS_PATH);
        if (logsSection == null) return;
        for (final var itemId : logsSection.getKeys(false)) {
            String strippedLogId = logsSection.getString(itemId + "." + STRIPPED_LOG_ID_PATH);
            if (strippedLogId == null) {
                strippedLogId = itemId;
            }
            final WrappedBlockState state = WrappedBlockState.getDefaultState(StateTypes.NOTE_BLOCK);
            final WrappedBlockState strippedLogState = WrappedBlockState.getDefaultState(StateTypes.NOTE_BLOCK);
            final Material logMaterial = this.loadMaterial(logsSection, itemId + "." + LOG_MATERIAL_PATH, this.defaultLogMaterial);
            final Material strippedLogMaterial = this.loadMaterial(logsSection, itemId + "." + STRIPPED_LOG_MATERIAL_PATH, this.defaultStrippedLogMaterial);
            try {
                final Instrument instrument = Instrument.valueOf(logsSection.getString(itemId + "." + INSTRUMENT_PATH).toUpperCase());
                final int note = logsSection.getInt(itemId + "." + NOTE_PATH);
                final Instrument strippedInstrument = Instrument.valueOf(logsSection.getString(itemId + "." + STRIPPED_INSTRUMENT_PATH).toUpperCase());
                final int strippedNote = logsSection.getInt(itemId + "." + STRIPPED_NOTE_PATH);
                state.setInstrument(instrument);
                state.setNote(note);
                strippedLogState.setInstrument(strippedInstrument);
                strippedLogState.setNote(strippedNote);
            } catch (IllegalArgumentException | NullPointerException e) {
                this.plugin.getLogger().severe("Invalid instrument or note for log " + itemId + " in config.yml");
            }
            final Supplier<ItemStack> itemStackSupplier = this.loadItemStack(
                    logsSection.getConfigurationSection(itemId),
                    itemId,
                    itemId
            );
            final Supplier<ItemStack> strippedItemStackSupplier = this.loadItemStack(
                    logsSection.getConfigurationSection(itemId),
                    strippedLogId,
                    strippedLogId
            );
            this.itemSupplierMap.put(itemId, itemStackSupplier);
            this.itemSupplierMap.put(strippedLogId, strippedItemStackSupplier);
            this.playerItemIds.add(itemId);
            this.playerItemIds.add(strippedLogId);
            for (Axis axis : Axis.values()) {
                final String directionalId = itemId + "_" + axis.name().toLowerCase();
                final String strippedDirectionalId = strippedLogId + "_" + axis.name().toLowerCase();
                final LogData blockData = BlockData.logData(
                        directionalId,
                        strippedDirectionalId,
                        state.getGlobalId(),
                        logMaterial,
                        strippedLogMaterial,
                        false,
                        strippedLogState.getGlobalId(),
                        axis
                );
                this.blockDataMap.put(directionalId, blockData);
                this.blockDataMap.put(strippedDirectionalId, blockData.strip());
                this.itemSupplierMap.put(directionalId, itemStackSupplier);
                this.itemSupplierMap.put(strippedDirectionalId, strippedItemStackSupplier);
            }
        }
        for (Material logMaterial : LOGS) {
            for (Axis axis : Axis.values()) {
                final String defaultLogStringId = getDefaultLogStringId(logMaterial) + "_" + axis.name().toLowerCase();
                final Material strippedLogMaterial = STRIPPED_LOGS.get(LOGS.indexOf(logMaterial));
                final String defaultStrippedLogStringId = getDefaultStrippedLogStringId(strippedLogMaterial) + "_" + axis.name().toLowerCase();
                final LogData blockData = BlockData.logData(
                        defaultLogStringId,
                        defaultStrippedLogStringId,
                        getLogById(getDefaultLogId(logMaterial)).getGlobalId(),
                        logMaterial,
                        strippedLogMaterial,
                        false,
                        getLogById(getDefaultStrippedLogId(logMaterial, false)).getGlobalId(),
                        axis
                );
                this.blockDataMap.put(defaultLogStringId, blockData);
                this.blockDataMap.put(defaultStrippedLogStringId, blockData.strip());
            }
        }
    }

    private static final String STAGE_PATH = "stage";
    private static final String SCHEMATIC_FILES_PATH = "schematic-files";
    private static final String RANDOM_PASTE_ROTATION_PATH = "random-paste-rotation";
    private static final String ALLOWED_SOIL_BLOCKS_PATH = "allowed-soil-blocks";

    private static final Predicate<Location> DEFAULT_SAPLING_PREDICATE = location -> Tag.DIRT.isTagged(location.getBlock().getType());

    private void loadSaplingsSection(FileConfiguration config) {
        final ConfigurationSection saplingsSection = config.getConfigurationSection(SAPLINGS_PATH);
        if (saplingsSection == null) return;
        for (final var itemId : saplingsSection.getKeys(false)) {
            final Supplier<ItemStack> itemStackSupplier = this.loadItemStack(
                    saplingsSection.getConfigurationSection(itemId),
                    itemId,
                    itemId
            );
            this.itemSupplierMap.put(itemId, itemStackSupplier);
            this.playerItemIds.add(itemId);
            final Material saplingMaterial;
            try {
                saplingMaterial = Material.valueOf(saplingsSection.getString(itemId + "." + SAPLING_MATERIAL_PATH).toUpperCase());
            } catch (IllegalArgumentException | NullPointerException e) {
                this.plugin.getLogger().severe("Invalid sapling material for sapling " + itemId + " in config.yml");
                return;
            }
            final int stage = saplingsSection.getInt(itemId + "." + STAGE_PATH);
            if (stage < 0 || stage > 1) {
                this.plugin.getLogger().severe("Invalid stage for sapling " + itemId + " in config.yml: " + stage);
                return;
            }
            final List<String> schematicFiles = saplingsSection.getStringList(itemId + "." + SCHEMATIC_FILES_PATH);
            final boolean randomPasteRotation = saplingsSection.getBoolean(itemId + "." + RANDOM_PASTE_ROTATION_PATH, false);
            final String modelPath = saplingsSection.getString(itemId + "." + MODEL_PATH_PATH);
            final ConfigurationSection predicateSection = saplingsSection.getConfigurationSection(itemId + "." + ALLOWED_SOIL_BLOCKS_PATH);
            final Predicate<Location> predicate;
            if (predicateSection == null) {
                predicate = DEFAULT_SAPLING_PREDICATE;
            } else {
                predicate = this.loadSaplingSoilPredicate(predicateSection);
            }
            final WrappedBlockState state = WrappedBlockState.getDefaultState(
                    PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                    SpigotConversionUtil.fromBukkitBlockData(saplingMaterial.createBlockData()).getType()
            );
            state.setStage(stage);
            final BlockData saplingData = BlockData.saplingData(
                    itemId,
                    state.getGlobalId(),
                    saplingMaterial,
                    schematicFiles,
                    randomPasteRotation,
                    modelPath
            );
            this.blockDataMap.put(itemId, saplingData);
            this.saplingSoilPredicateMap.put(itemId, predicate);
        }
        for (Material sapling : SAPLINGS) {
            final WrappedBlockState state = WrappedBlockState.getDefaultState(
                    PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                    SpigotConversionUtil.fromBukkitBlockData(sapling.createBlockData()).getType()
            );
            final String defaultSaplingId = getDefaultSaplingStringId(sapling);
            this.blockDataMap.put(
                    defaultSaplingId,
                    BlockData.saplingData(
                            defaultSaplingId,
                            state.getGlobalId(),
                            sapling,
                            new ArrayList<>(),
                            false,
                            null
                    )
            );
            this.saplingSoilPredicateMap.put(defaultSaplingId, DEFAULT_SAPLING_PREDICATE);
        }
    }

    private static final String WITH_GLOW_BERRY_ID_PATH = "with-glow-berry-id";

    private void loadCaveVinesSection(ConfigurationSection config) {
        final ConfigurationSection caveVinesSection = config.getConfigurationSection(CAVE_VINES_PATH);
        if (caveVinesSection == null) return;
        for (final var itemId : caveVinesSection.getKeys(false)) {
            final String withGlowBerryId = caveVinesSection.getString(itemId + "." + WITH_GLOW_BERRY_ID_PATH);
            if (withGlowBerryId == null) {
                this.plugin.getLogger().severe("Missing with-glow-berry-id for cave vines " + itemId + " in config.yml");
                continue;
            }
            final Supplier<ItemStack> itemStackSupplier = this.loadItemStack(
                    caveVinesSection.getConfigurationSection(itemId),
                    itemId,
                    itemId
            );
            final Supplier<ItemStack> withGlowBerryItemStackSupplier = this.loadItemStack(
                    caveVinesSection.getConfigurationSection(itemId),
                    withGlowBerryId,
                    withGlowBerryId
            );
            this.itemSupplierMap.put(itemId, itemStackSupplier);
            this.itemSupplierMap.put(withGlowBerryId, withGlowBerryItemStackSupplier);
            this.playerItemIds.add(itemId);
            this.playerItemIds.add(withGlowBerryId);
            final String modelPath = caveVinesSection.getString(itemId + "." + MODEL_PATH_PATH);
            final int stateId = caveVinesSection.getInt(itemId + "." + STATE_ID_PATH);
            final CaveVineData blockData = BlockData.caveVineData(
                    itemId,
                    withGlowBerryId,
                    getCaveVinesById(stateId).getGlobalId(),
                    false,
                    modelPath
            );
            this.blockDataMap.put(itemId, blockData);
            this.blockDataMap.put(withGlowBerryId, blockData.withGlowBerry(true));
        }
        final String defaultCaveVinesStringId = getDefaultCaveVinesStringId(false);
        final String defaultCaveVinesStringIdWithBerries = getDefaultCaveVinesStringId(true);
        final CaveVineData defaultCaveVineData = BlockData.caveVineData(
                defaultCaveVinesStringId,
                defaultCaveVinesStringIdWithBerries,
                getCaveVinesById(1).getGlobalId(),
                false,
                null
        );
        this.blockDataMap.put(
                defaultCaveVinesStringId,
                defaultCaveVineData
        );
        this.blockDataMap.put(
                defaultCaveVinesStringIdWithBerries,
                defaultCaveVineData.withGlowBerry(true)
        );
    }

    private static final String TAGS_PATH = "tags";
    private static final String HOOK_BLOCK_IDS_PATH = "hook-block-ids";
    private static final String MATERIALS_PATH = "materials";

    private Predicate<Location> loadSaplingSoilPredicate(ConfigurationSection predicateSection) {
        final Set<Tag<Material>> tags = predicateSection.getStringList(TAGS_PATH).
                stream()
                .map(s -> Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(s), Material.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        final Set<String> hookBlockIds = new HashSet<>(predicateSection.getStringList(HOOK_BLOCK_IDS_PATH));
        final Set<Material> materialStrings = predicateSection.getStringList(MATERIALS_PATH)
                .stream()
                .map(s -> {
                    try {
                        return Material.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return location -> {
            final Material material = location.getBlock().getType();
            if (materialStrings.contains(material)) return true;
            for (Tag<Material> tag : tags) {
                if (tag.isTagged(material)) return true;
            }
            return hookBlockIds.contains(Hooks.getCustomBlockIdAt(location));
        };
    }

    private Material loadMaterial(ConfigurationSection section, String path, Material defaultMaterial) {
        try {
            return Material.valueOf(section.getString(path));
        } catch (IllegalArgumentException e) {
            return defaultMaterial;
        }
    }

    private static final String MATERIAL_PATH = "material";
    private static final String AMOUNT_PATH = "amount";
    private static final String NAME_PATH = "name";
    private static final String LORE_PATH = "lore";
    private static final String MODEL_DATA_PATH = "model-data";

    private Supplier<ItemStack> loadItemStack(ConfigurationSection section, String itemId, String hookId) {
        final String materialStr = section.getString(MATERIAL_PATH);
        Material material;
        try {
            material = Material.valueOf(materialStr.toUpperCase());
        } catch (NullPointerException | IllegalArgumentException e) {
//            throw new IllegalStateException("Material not found for " + materialStr + " item.");
            material = Material.AIR;
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
        if (hookId != null && Hooks.hasOtherItemHook()) {
            return () -> {
                final ItemStack hookItemStack = Hooks.getItem(hookId);
                return Objects.requireNonNullElse(hookItemStack, itemStack).clone();
            };
        }
        return itemStack::clone;
    }

}
