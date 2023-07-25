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
import com.github.retrooper.packetevents.protocol.sound.SoundCategory;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.enums.Instrument;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.data.BlockDataSound;
import io.github.fisher2911.hmcleaves.data.CaveVineData;
import io.github.fisher2911.hmcleaves.data.LimitedStacking;
import io.github.fisher2911.hmcleaves.data.LogData;
import io.github.fisher2911.hmcleaves.data.SoundData;
import io.github.fisher2911.hmcleaves.hook.Hooks;
import io.github.fisher2911.hmcleaves.packet.BlockBreakModifier;
import io.github.fisher2911.hmcleaves.util.ChainedBlockUtil;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.fisher2911.hmcleaves.world.Position;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.CaveVinesPlant;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
    private static final String DEFAULT_AGEABLE_ID = "default_ageable_id";

    private static final int STATES_PER_LEAF = 7 * 2;
    private static final List<Material> LEAVES = new ArrayList<>();
    private static final List<Material> LOGS = new ArrayList<>();
    private static final List<Material> STRIPPED_LOGS = new ArrayList<>();
    private static final List<Material> SAPLINGS = new ArrayList<>();

    public static final Set<Material> AGEABLE_MATERIALS = Set.of(
            Material.SUGAR_CANE,
            Material.KELP,
            Material.KELP_PLANT,
            Material.TWISTING_VINES,
            Material.TWISTING_VINES_PLANT,
            Material.WEEPING_VINES,
            Material.WEEPING_VINES_PLANT
    );

    public static final Set<Material> SHEAR_STOPS_GROWING_MATERIALS = Set.of(
            Material.KELP,
            Material.CAVE_VINES,
            Material.TWISTING_VINES,
            Material.WEEPING_VINES
    );


    private static WrappedBlockState getLeafById(int id) {
        if (id < 0) {
            throw new IllegalStateException("Leaf id must be 0 or greater!");
        }
        final int materialId = id / STATES_PER_LEAF;
        final int distance = id % 7 + 1;
        final boolean persistent = id % STATES_PER_LEAF >= STATES_PER_LEAF / 2;
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


    private static WrappedBlockState getCaveVinesById(int id) {
        final boolean berries = id > 25;
        final int age = id % 25 - 1;
        final WrappedBlockState state = StateTypes.CAVE_VINES.createBlockState();
        state.setBerries(berries);
        state.setAge(age);
        return state;
    }

    private static WrappedBlockState getAgeableById(Material material, int id) {
        final var state = SpigotConversionUtil.fromBukkitBlockData(material.createBlockData()).clone();
        state.setAge(id);
        return state;
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

        // 1.20 leaves / logs
        try {
            LEAVES.add(Material.valueOf("CHERRY_LEAVES"));

            LOGS.add(Material.valueOf("CHERRY_LOG"));
            LOGS.add(Material.valueOf("CHERRY_WOOD"));
            STRIPPED_LOGS.add(Material.valueOf("STRIPPED_CHERRY_LOG"));
            STRIPPED_LOGS.add(Material.valueOf("STRIPPED_CHERRY_WOOD"));
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

    public static String getDefaultLogStringId(Material material, Axis axis) {
        return DEFAULT_LOG_ID + "_" + material.name().toLowerCase() + "_" + axis.name().toLowerCase();
    }

    public static String getDefaultStrippedLogStringId(Material material) {
        return DEFAULT_STRIPPED_LOG_ID + "_" + material.name().toLowerCase();
    }

    public static String getDefaultStrippedLogStringId(Material material, Axis axis) {
        return DEFAULT_STRIPPED_LOG_ID + "_" + material.name().toLowerCase() + "_" + axis.name().toLowerCase();
    }

    public static String getDefaultSaplingStringId(Material material) {
        return DEFAULT_SAPLING_ID + "_" + material.name().toLowerCase();
    }

    public static String getDefaultCaveVinesStringId(boolean glowBerries) {
        return DEFAULT_CAVE_VINES_ID + "_" + (glowBerries ? "glow" : "normal");
    }

    public static String getDefaultAgeableStringId(Material material) {
        return DEFAULT_AGEABLE_ID + "_" + material.name().toLowerCase();
    }

    @Nullable
    public static String getDefaultBlockStringId(org.bukkit.block.data.BlockData blockData) {
        final Material material = blockData.getMaterial();
        if (Tag.LEAVES.isTagged(material)) {
            return getDefaultLeafStringId(material);
        }
        if (Tag.SAPLINGS.isTagged(material)) {
            return getDefaultSaplingStringId(material);
        }
        if (Tag.LOGS.isTagged(material)) {
            final Orientable orientable = (Orientable) blockData;
            if (material.toString().contains("STRIPPED")) {
                return getDefaultStrippedLogStringId(material) + "_" + orientable.getAxis().name().toLowerCase();
            }
            return getDefaultLogStringId(material) + "_" + orientable.getAxis().name().toLowerCase();
        }
        if (Tag.CAVE_VINES.isTagged(material)) {
            return getDefaultCaveVinesStringId(((CaveVinesPlant) blockData).isBerries());
        }
        if (AGEABLE_MATERIALS.contains(material)) {
            return getDefaultAgeableStringId(material);
        }
        return null;
    }

    private final HMCLeaves plugin;
    private final Path itemsFolderPath;
    private final TextureFileGenerator textureFileGenerator;
    private final Map<String, BlockData> blockDataMap;
    // so that tab complete doesn't show directional ID's
    private final Set<String> playerItemIds;
    private final Map<String, Supplier<ItemStack>> itemSupplierMap;
    private final Map<String, Supplier<ItemStack>> saplingItemSupplierMap;
    private final Map<String, Supplier<ItemStack>> leafDropItemSupplierMap;
    // what blocks a sapling can be placed on
    private final Map<String, Predicate<Block>> blockSupportPredicateMap;
    private Material defaultLeafMaterial = Material.OAK_LEAVES;
    private Material defaultLogMaterial = Material.OAK_LOG;
    private Material defaultStrippedLogMaterial = Material.STRIPPED_OAK_LOG;

    private int chunkVersion;
    private boolean useWorldWhitelist;
    private Set<String> whitelistedWorlds;

    public LeavesConfig(
            HMCLeaves plugin,
            Map<String, BlockData> blockDataMap,
            Map<String, Supplier<ItemStack>> itemSupplierMap,
            Map<String, Supplier<ItemStack>> saplingItemSupplierMap,
            Map<String, Supplier<ItemStack>> leafDropItemSupplierMap,
            Map<String, Predicate<Block>> blockSupportPredicateMap
    ) {
        this.plugin = plugin;
        this.textureFileGenerator = new TextureFileGenerator(plugin);
        this.playerItemIds = new HashSet<>();
        this.blockDataMap = blockDataMap;
        this.itemSupplierMap = itemSupplierMap;
        this.saplingItemSupplierMap = saplingItemSupplierMap;
        this.leafDropItemSupplierMap = leafDropItemSupplierMap;
        this.blockSupportPredicateMap = blockSupportPredicateMap;
        this.itemsFolderPath = this.plugin.getDataFolder().toPath()
                .resolve("items");
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
    public BlockData getDefaultBlockData(org.bukkit.block.data.BlockData blockData) {
        final Material material = blockData.getMaterial();
        if (Tag.LEAVES.isTagged(material)) {
            return getDefaultLeafData(material);
        }
        if (Tag.SAPLINGS.isTagged(material)) {
            return getDefaultSaplingData(material);
        }
        if (Tag.LOGS.isTagged(material)) {
            final Orientable orientable = (Orientable) blockData;
            if (material.toString().contains("STRIPPED")) {
                return getDefaultStrippedLogData(material, orientable.getAxis());
            }
            return getDefaultLogData(material, orientable.getAxis());
        }
        if (Tag.CAVE_VINES.isTagged(material)) {
            return getDefaultCaveVinesData(((CaveVinesPlant) blockData).isBerries());
        }
        if (AGEABLE_MATERIALS.contains(material)) {
            return getDefaultAgeableData(material);
        }
        return null;
    }

    private static final Map<Material, Material> AGEABLE_MATERIAL_CONVERSION = Map.of(
            Material.KELP_PLANT, Material.KELP,
            Material.TWISTING_VINES_PLANT, Material.TWISTING_VINES,
            Material.WEEPING_VINES_PLANT, Material.WEEPING_VINES
    );

    @Nullable
    public BlockData getDefaultAgeableData(Material material) {
        final Material actualMaterial = AGEABLE_MATERIAL_CONVERSION.getOrDefault(material, material);
        return this.blockDataMap.get(getDefaultAgeableStringId(actualMaterial));
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
    private static final String SUGAR_CANE_PATH = "sugar-cane";
    private static final String KELP_PATH = "kelp";
    private static final String WEEPING_VINES_PATH = "weeping-vines";
    private static final String TWISTING_VINES_PATH = "twisting-vines";


    private static final String LEAF_MATERIAL_PATH = "leaf-material";
    private static final String LOG_MATERIAL_PATH = "log-material";
    private static final String WORLD_PERSISTENCE_PATH = "world-persistence";
    private static final String STRIPPED_LOG_MATERIAL_PATH = "stripped-log-material";
    private static final String STATE_ID_PATH = "state-id";
    private static final String TIPPED_STATE_ID_PATH = "tipped-state-id";
    private static final String STACK_LIMIT_PATH = "stack-limit";
    private static final String SHOULD_GROW_BERRIES_PATH = "should-grow-berries";
    private static final String SAPLING_MATERIAL_PATH = "sapling-material";

    private static final String SAPLING_PATH = "sapling";
    private static final String LEAF_DROP_REPLACEMENT_PATH = "leaf-drop-replacement";

    private static final String MODEL_PATH_PATH = "model-path";

    private static final String SUPPORTABLE_FACES_PATH = "supportable-faces";

    private static final String ONLY_FOLLOW_WORLD_PERSISTENCE_IF_CONNECTED_TO_LOG_PATH = "only-follow-world-persistence-if-connected-to-log";
    private boolean onlyFollowWorldPersistenceIfConnectedToLog;

    private static final String CHUNK_VERSION_PATH = "chunk-version";
    private static final String USE_WORLD_WHITELIST_PATH = "use-world-whitelist";
    private static final String WHITELISTED_WORLDS_PATH = "whitelisted-worlds";

    private static final Collection<String> DEFAULT_FILE_NAMES = List.of(
            "cave-vines.yml",
            "kelp.yml",
            "leaves.yml",
            "logs.yml",
            "saplings.yml",
            "sugar-cane.yml",
            "twisting-vines.yml",
            "weeping-vines.yml"
    );

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
        if (!config.contains(CHUNK_VERSION_PATH)) {
            config.set(CHUNK_VERSION_PATH, 1);
            this.plugin.saveConfig();
        }
        this.chunkVersion = config.getInt(CHUNK_VERSION_PATH);
        initLeavesAndLogs();
        final File itemsFolder = this.itemsFolderPath.toFile();
        if (!itemsFolder.exists() && !itemsFolder.mkdirs()) {
            throw new IllegalStateException("Could not create items folder");
        }
        final File[] files = itemsFolder.listFiles();
        if (files == null) {
            return;
        }
        boolean createDefaults = true;
        for (final File file : files) {
            if (!DEFAULT_FILE_NAMES.contains(file)) {
                createDefaults = false;
                break;
            }
        }
        if (createDefaults) {
            for (final String fileName : DEFAULT_FILE_NAMES) {
                final File defaultFile = new File(itemsFolder, fileName);
                if (!defaultFile.exists()) {
                    this.plugin.saveResource("items/" + fileName, false);
                }
            }
        }
        this.loadLeavesSection(config);
        this.loadLogsSection(config);
        this.loadSaplingsSection(config);
        this.loadCaveVinesSection(config);
        this.loadAgeableSections(config);
        for (final File file : files) {
            final YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);
            this.loadLeavesSection(fileConfig);
            this.loadLogsSection(fileConfig);
            this.loadSaplingsSection(fileConfig);
            this.loadCaveVinesSection(fileConfig);
            this.loadAgeableSections(fileConfig);
        }
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
        for (Material log : LOGS) {
            this.textureFileGenerator.generateFile(
                    Material.NOTE_BLOCK,
                    this.blockDataMap.values().stream()
                            .filter(LogData.class::isInstance)
                            .filter(blockData -> blockData.realBlockType() == log)
                            .filter(blockData -> blockData.modelPath() != null)
                            .collect(Collectors.toList())
            );
        }
        for (Material log : STRIPPED_LOGS) {
            this.textureFileGenerator.generateFile(
                    Material.NOTE_BLOCK,
                    this.blockDataMap.values().stream()
                            .filter(LogData.class::isInstance)
                            .filter(blockData -> ((LogData) blockData).strippedBlockType() == log)
                            .filter(blockData -> blockData.modelPath() != null)
                            .collect(Collectors.toList())
            );
        }
        this.loadDefaults();
    }

    public int getChunkVersion() {
        return this.chunkVersion;
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

    public boolean canPlaceBlockAgainst(BlockData blockData, Block block) {
        final Predicate<Block> predicate = this.blockSupportPredicateMap.get(blockData.id());
        if (predicate == null) return true;
        for (final BlockFace blockFace : blockData.supportableFaces()) {
            if (predicate.test(block.getRelative(blockFace))) {
                if (blockData instanceof final LimitedStacking limitedStacking) {
                    final int stack = ChainedBlockUtil.countStack(
                            Position.fromLocation(block.getLocation()),
                            blockData,
                            this.plugin.getBlockCache()
                    );
                    return stack < limitedStacking.stackLimit();
                }
                return true;
            }
        }
        return false;
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

    private static final String ALLOWED_SUPPORTABLE_BLOCKS = "allowed-supportable-blocks";

    private static final Set<BlockFace> DEFAULT_BLOCK_SUPPORTABLE_FACES = Arrays.stream(BlockFace.values())
            .filter(BlockFace::isCartesian)
            .collect(Collectors.toUnmodifiableSet());

    private void loadDefaults() {
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
                    null,
                    DEFAULT_BLOCK_SUPPORTABLE_FACES,
                    null,
                    null
            ));
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
                        axis,
                        null,
                        DEFAULT_BLOCK_SUPPORTABLE_FACES,
                        null
                );
                this.blockDataMap.put(defaultLogStringId, blockData);
                this.blockDataMap.put(defaultStrippedLogStringId, blockData.strip());
            }
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
                            null,
                            null
                    )
            );
            this.blockSupportPredicateMap.put(defaultSaplingId, DEFAULT_SAPLING_PREDICATE);
        }

        final String defaultCaveVinesStringId = getDefaultCaveVinesStringId(false);
        final String defaultCaveVinesStringIdWithBerries = getDefaultCaveVinesStringId(true);
        final int defaultCaveVineSendId = getCaveVinesById(1).getGlobalId();
        final CaveVineData defaultCaveVineData = BlockData.caveVineData(
                defaultCaveVinesStringId,
                defaultCaveVinesStringIdWithBerries,
                defaultCaveVineSendId,
                defaultCaveVineSendId,
                false,
                null,
                Integer.MAX_VALUE,
                DEFAULT_CAVE_VINES_SUPPORTABLE_FACES,
                null,
                true,
                null
        );
        this.blockSupportPredicateMap.put(defaultCaveVinesStringId, DEFAULT_CAVE_VINES_PREDICATE);
        this.blockSupportPredicateMap.put(defaultCaveVinesStringIdWithBerries, DEFAULT_CAVE_VINES_PREDICATE);
        this.blockDataMap.put(
                defaultCaveVinesStringId,
                defaultCaveVineData
        );
        this.blockDataMap.put(
                defaultCaveVinesStringIdWithBerries,
                defaultCaveVineData.withGlowBerry(true)
        );

        this.loadAgeableDefaults(
                Material.SUGAR_CANE,
                Sound.BLOCK_GRASS_PLACE,
                DEFAULT_SUGAR_CANE_SUPPORTABLE_FACES,
                m -> m == Material.SUGAR_CANE,
                DEFAULT_SUGAR_CANE_PREDICATE,
                Material.SUGAR_CANE,
                Material.AIR
        );
        this.loadAgeableDefaults(
                Material.KELP,
                Sound.BLOCK_GRASS_PLACE,
                DEFAULT_KELP_SUPPORTABLE_FACES,
                m -> m == Material.KELP_PLANT || m == Material.KELP,
                DEFAULT_KELP_PREDICATE,
                Material.KELP_PLANT,
                Material.WATER
        );
        this.loadAgeableDefaults(
                Material.TWISTING_VINES,
                Sound.BLOCK_VINE_PLACE,
                Set.of(BlockFace.DOWN),
                m -> m == Material.TWISTING_VINES || m == Material.TWISTING_VINES_PLANT,
                DEFAULT_TWISTING_VINES_PREDICATE,
                Material.TWISTING_VINES_PLANT,
                Material.AIR
        );
        this.loadAgeableDefaults(
                Material.WEEPING_VINES,
                Sound.BLOCK_WEEPING_VINES_PLACE,
                Set.of(BlockFace.UP),
                m -> m == Material.WEEPING_VINES || m == Material.WEEPING_VINES_PLANT,
                DEFAULT_WEEPING_VINES_PREDICATE,
                Material.WEEPING_VINES_PLANT,
                Material.AIR
        );
    }

    private void loadAgeableDefaults(
            Material ageableMaterial,
            Sound placeSound,
            Set<BlockFace> supportableFaces,
            Predicate<Material> worldTypeSamePredicate,
            Predicate<Block> defaultPlacePredicate,
            Material defaultLowerMaterial,
            Material breakReplacement
    ) {
        final String defaultStringId = getDefaultAgeableStringId(ageableMaterial);
        final int defaultSendId = getAgeableById(ageableMaterial, 0).getGlobalId();
        final var defaultData = BlockData.ageableData(
                defaultStringId,
                ageableMaterial,
                defaultSendId,
                defaultSendId,
                null,
                placeSound,
                worldTypeSamePredicate,
                defaultLowerMaterial,
                Integer.MAX_VALUE,
                breakReplacement,
                supportableFaces,
                null
        );
        this.blockDataMap.put(defaultStringId, defaultData);
        this.blockSupportPredicateMap.put(defaultStringId, defaultPlacePredicate);
    }

    private void loadLeavesSection(FileConfiguration config) {
        final ConfigurationSection leavesSection = config.getConfigurationSection(LEAVES_PATH);
        if (leavesSection == null) return;
        for (var itemId : leavesSection.getKeys(false)) {
            final Supplier<ItemStack> itemStackSupplier = this.loadItemStack(
                    leavesSection.getConfigurationSection(itemId),
                    itemId,
                    itemId
            );
            this.itemSupplierMap.put(itemId, itemStackSupplier);
            this.playerItemIds.add(itemId);
            final int stateId = leavesSection.getInt(itemId + "." + STATE_ID_PATH);
            final WrappedBlockState byIdState = getLeafById(stateId);
            final Material leafMaterial = this.loadMaterial(leavesSection, itemId + "." + LEAF_MATERIAL_PATH, this.defaultLeafMaterial);
            final WrappedBlockState state = WrappedBlockState.getDefaultState(
                    SpigotConversionUtil.fromBukkitItemMaterial(leafMaterial).getPlacedType()
            );
            state.setDistance(byIdState.getDistance());
            state.setPersistent(byIdState.isPersistent());
            final boolean worldPersistence = leavesSection.getBoolean(itemId + "." + WORLD_PERSISTENCE_PATH, state.isPersistent());
            final String modelPath = leavesSection.getString(itemId + "." + MODEL_PATH_PATH, null);
            final Set<BlockFace> supportableFaces = this.loadSupportableFaces(leavesSection.getConfigurationSection(itemId), DEFAULT_BLOCK_SUPPORTABLE_FACES);
            final BlockBreakModifier blockBreakModifier = this.loadBlockBreakModifier(leavesSection.getConfigurationSection(itemId));
            final BlockDataSound sound = this.loadBlockDataSound(leavesSection.getConfigurationSection(itemId));
            final BlockData blockData = BlockData.leafData(
                    itemId,
                    state.getGlobalId(),
                    leafMaterial,
                    state.getDistance(),
                    state.isPersistent(),
                    worldPersistence,
                    false,
                    modelPath,
                    supportableFaces,
                    blockBreakModifier,
                    sound
            );
            final ConfigurationSection predicateSection = leavesSection.getConfigurationSection(itemId + "." + ALLOWED_SUPPORTABLE_BLOCKS);
            if (predicateSection != null) {
                final Predicate<Block> predicate = this.loadSupportableBlockPredicate(predicateSection);
                this.blockSupportPredicateMap.put(itemId, predicate);
            }
            this.blockDataMap.put(itemId, blockData);
            this.loadSapling(leavesSection.getConfigurationSection(itemId), itemId);
            this.loadLeafDropReplacement(leavesSection.getConfigurationSection(itemId), itemId);
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
    private static final String STRIPPED_LOG_ITEM_PATH = "stripped-log";
    private static final String GENERATE_AXES_PATH = "generate-axes";

    private void loadLogsSection(FileConfiguration config) {
        final ConfigurationSection logsSection = config.getConfigurationSection(LOGS_PATH);
        if (logsSection == null) return;
        for (final var itemId : logsSection.getKeys(false)) {
            String strippedLogId = logsSection.getString(itemId + "." + STRIPPED_LOG_ID_PATH);
            final boolean generateAxes = logsSection.getBoolean(itemId + "." + GENERATE_AXES_PATH, false);
            if (strippedLogId == null) {
                strippedLogId = itemId;
            }
            final WrappedBlockState state = WrappedBlockState.getDefaultState(StateTypes.NOTE_BLOCK);
            final WrappedBlockState strippedLogState = WrappedBlockState.getDefaultState(StateTypes.NOTE_BLOCK);
            final Material logMaterial = this.loadMaterial(logsSection, itemId + "." + LOG_MATERIAL_PATH, this.defaultLogMaterial);
            final Material strippedLogMaterial = this.loadMaterial(logsSection, itemId + "." + STRIPPED_LOG_MATERIAL_PATH, this.defaultStrippedLogMaterial);
            final String modelPath = logsSection.getString(itemId + "." + MODEL_PATH_PATH, null);
            Instrument instrument = INSTRUMENTS.get(0);
            int note = 0;
            Instrument strippedInstrument = INSTRUMENTS.get(0);
            int strippedNote = 3;
            try {
                instrument = Instrument.valueOf(logsSection.getString(itemId + "." + INSTRUMENT_PATH).toUpperCase());
                note = logsSection.getInt(itemId + "." + NOTE_PATH);
                strippedInstrument = Instrument.valueOf(logsSection.getString(itemId + "." + STRIPPED_INSTRUMENT_PATH).toUpperCase());
                strippedNote = logsSection.getInt(itemId + "." + STRIPPED_NOTE_PATH);
            } catch (IllegalArgumentException | NullPointerException e) {
                this.plugin.getLogger().warning("Invalid instrument or note for log " + itemId + " in config.yml, " +
                        "make sure you are using a hook if this is intentional");
            }
            state.setInstrument(instrument);
            state.setNote(note);
            strippedLogState.setInstrument(strippedInstrument);
            strippedLogState.setNote(strippedNote);
            final Supplier<ItemStack> itemStackSupplier = this.loadItemStack(
                    logsSection.getConfigurationSection(itemId),
                    itemId,
                    itemId
            );
            final Supplier<ItemStack> strippedItemStackSupplier;
            if (logsSection.getConfigurationSection(itemId).getConfigurationSection(STRIPPED_LOG_ITEM_PATH) != null) {
                strippedItemStackSupplier = this.loadItemStack(
                        logsSection.getConfigurationSection(itemId).getConfigurationSection(STRIPPED_LOG_ITEM_PATH),
                        strippedLogId,
                        strippedLogId
                );
            } else {
                strippedItemStackSupplier = this.loadItemStack(
                        logsSection.getConfigurationSection(itemId),
                        strippedLogId,
                        strippedLogId
                );
            }
            final Set<BlockFace> supportableFaces = this.loadSupportableFaces(logsSection.getConfigurationSection(itemId), DEFAULT_BLOCK_SUPPORTABLE_FACES);
            this.itemSupplierMap.put(itemId, itemStackSupplier);
            this.itemSupplierMap.put(strippedLogId, strippedItemStackSupplier);
            this.playerItemIds.add(itemId);
            this.playerItemIds.add(strippedLogId);
            final ConfigurationSection predicateSection = logsSection.getConfigurationSection(itemId + "." + ALLOWED_SUPPORTABLE_BLOCKS);
            if (predicateSection != null) {
                final Predicate<Block> predicate = this.loadSupportableBlockPredicate(predicateSection);
                this.blockSupportPredicateMap.put(itemId, predicate);
            }
            final BlockDataSound sound = this.loadBlockDataSound(logsSection.getConfigurationSection(itemId));
            Integer nextNote = null;
            Instrument nextInstrument = null;
            Integer nextStrippedNote = null;
            Instrument nextStrippedInstrument = null;
            for (Axis axis : Axis.values()) {
                final String directionalId = itemId + "_" + axis.name().toLowerCase();
                final String strippedDirectionalId = strippedLogId + "_" + axis.name().toLowerCase();
                if (generateAxes) {
                    if (nextNote != null) {
                        nextNote = getNextNote(nextNote);
                    } else {
                        nextNote = note;
                    }
                    state.setNote(nextNote);
                    if (nextNote == 0) {
                        if (nextInstrument != null) {
                            nextInstrument = getNextInstrument(nextInstrument);
                        } else {
                            nextInstrument = instrument;
                        }
                        state.setInstrument(nextInstrument);
                    }
                    if (nextStrippedNote != null) {
                        nextStrippedNote = getNextNote(nextStrippedNote);
                    } else {
                        nextStrippedNote = strippedNote;
                    }
                    strippedLogState.setNote(nextStrippedNote);
                    if (nextStrippedNote == 0) {
                        if (nextStrippedInstrument != null) {
                            nextStrippedInstrument = getNextInstrument(nextStrippedInstrument);
                        } else {
                            nextStrippedInstrument = strippedInstrument;
                        }
                        strippedLogState.setInstrument(nextStrippedInstrument);
                    }
                }
                final LogData blockData = BlockData.logData(
                        directionalId,
                        strippedDirectionalId,
                        state.getGlobalId(),
                        logMaterial,
                        strippedLogMaterial,
                        false,
                        strippedLogState.getGlobalId(),
                        axis,
                        modelPath,
                        supportableFaces,
                        sound
                );
                if (predicateSection != null) {
                    final Predicate<Block> predicate = this.loadSupportableBlockPredicate(predicateSection);
                    this.blockSupportPredicateMap.put(directionalId, predicate);
                    this.blockSupportPredicateMap.put(strippedDirectionalId, predicate);
                }
                this.blockDataMap.put(directionalId, blockData);
                this.blockDataMap.put(strippedDirectionalId, blockData.strip());
                this.itemSupplierMap.put(directionalId, itemStackSupplier);
                this.itemSupplierMap.put(strippedDirectionalId, strippedItemStackSupplier);
            }
        }
    }

    private static int getNextNote(int i) {
        if (i == 24) {
            return 0;
        }
        return i + 1;
    }

    private static final List<Instrument> INSTRUMENTS = List.of(
            Instrument.BANJO,
            Instrument.BASEDRUM,
            Instrument.BASS,
            Instrument.BELL,
            Instrument.BIT,
            Instrument.CHIME,
            Instrument.COW_BELL,
            Instrument.DIDGERIDOO,
            Instrument.FLUTE,
            Instrument.GUITAR,
            Instrument.HARP,
            Instrument.HAT,
            Instrument.IRON_XYLOPHONE,
            Instrument.PLING,
            Instrument.SNARE,
            Instrument.XYLOPHONE,
            Instrument.ZOMBIE,
            Instrument.SKELETON,
            Instrument.CREEPER,
            Instrument.DRAGON,
            Instrument.WITHER_SKELETON,
            Instrument.PIGLIN,
            Instrument.CUSTOM_HEAD
    );

    private static Instrument getNextInstrument(Instrument instrument) {
        final int index = INSTRUMENTS.indexOf(instrument);
        if (index >= INSTRUMENTS.size()) {
            throw new IllegalArgumentException("Invalid instrument index " + index);
        }
        return INSTRUMENTS.get(index + 1);
    }

    private static final String STAGE_PATH = "stage";
    private static final String SCHEMATIC_FILES_PATH = "schematic-files";
    private static final String RANDOM_PASTE_ROTATION_PATH = "random-paste-rotation";

    private static final Predicate<Block> DEFAULT_SAPLING_PREDICATE = block -> Tag.DIRT.isTagged(block.getType());

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
            final ConfigurationSection predicateSection = saplingsSection.getConfigurationSection(itemId + "." + ALLOWED_SUPPORTABLE_BLOCKS);
            final Predicate<Block> predicate;
            if (predicateSection == null) {
                predicate = DEFAULT_SAPLING_PREDICATE;
            } else {
                predicate = this.loadSupportableBlockPredicate(predicateSection);
            }
            final WrappedBlockState state = WrappedBlockState.getDefaultState(
                    PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                    SpigotConversionUtil.fromBukkitBlockData(saplingMaterial.createBlockData()).getType()
            );
            state.setStage(stage);
            final BlockDataSound sound = this.loadBlockDataSound(saplingsSection.getConfigurationSection(itemId));
            final BlockData saplingData = BlockData.saplingData(
                    itemId,
                    state.getGlobalId(),
                    saplingMaterial,
                    schematicFiles,
                    randomPasteRotation,
                    modelPath,
                    sound
            );
            this.blockDataMap.put(itemId, saplingData);
            this.blockSupportPredicateMap.put(itemId, predicate);
        }
    }

    private static final String WITH_GLOW_BERRY_ID_PATH = "with-glow-berry-id";
    private static final String GLOW_BERRY_ITEM_PATH = "glow_berry";
    private static final Predicate<Block> DEFAULT_CAVE_VINES_PREDICATE = block ->
            block.getType().isSolid() || Tag.CAVE_VINES.isTagged(block.getType());
    private static final Set<BlockFace> DEFAULT_CAVE_VINES_SUPPORTABLE_FACES = Set.of(BlockFace.UP);

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
            final int stackLimit = caveVinesSection.getInt(itemId + "." + STACK_LIMIT_PATH, Integer.MAX_VALUE);
            final Set<BlockFace> supportableFaces = this.loadSupportableFaces(caveVinesSection.getConfigurationSection(itemId), DEFAULT_CAVE_VINES_SUPPORTABLE_FACES);
            final BlockDataSound sound = this.loadBlockDataSound(caveVinesSection.getConfigurationSection(itemId));
            final boolean shouldGrowBerries = caveVinesSection.getBoolean(itemId + "." + SHOULD_GROW_BERRIES_PATH, true);
            final int tippedStateId;
            final ConfigurationSection glowBerryItemSection = caveVinesSection.getConfigurationSection(itemId + "." + GLOW_BERRY_ITEM_PATH);
            final Supplier<ItemStack> glowBerryItemSupplier;
            if (glowBerryItemSection == null) {
                final String glowBerryItemId = caveVinesSection.getString(itemId + "." + GLOW_BERRY_ITEM_PATH);
                if (glowBerryItemId == null) {
                    glowBerryItemSupplier = null;
                } else {
                    glowBerryItemSupplier = () -> Hooks.getItem(glowBerryItemId);
                }
            } else {
                glowBerryItemSupplier = this.loadItemStack(glowBerryItemSection, itemId, itemId);
            }

            if (caveVinesSection.contains(itemId + "." + TIPPED_STATE_ID_PATH)) {
                tippedStateId = caveVinesSection.getInt(itemId + "." + TIPPED_STATE_ID_PATH);
            } else {
                tippedStateId = stateId;
            }
            final CaveVineData blockData = BlockData.caveVineData(
                    itemId,
                    withGlowBerryId,
                    getCaveVinesById(stateId).getGlobalId(),
                    getCaveVinesById(tippedStateId).getGlobalId(),
                    false,
                    modelPath,
                    stackLimit,
                    supportableFaces,
                    sound,
                    shouldGrowBerries,
                    glowBerryItemSupplier
            );
            final ConfigurationSection predicateSection = caveVinesSection.getConfigurationSection(itemId + "." + ALLOWED_SUPPORTABLE_BLOCKS);
            final Predicate<Block> placePredicate;
            if (predicateSection != null) {
                placePredicate = this.loadSupportableBlockPredicate(predicateSection);
            } else {
                placePredicate = DEFAULT_CAVE_VINES_PREDICATE;
            }
            this.blockSupportPredicateMap.put(itemId, placePredicate);
            this.blockSupportPredicateMap.put(withGlowBerryId, placePredicate);
            this.blockDataMap.put(itemId, blockData);
            this.blockDataMap.put(withGlowBerryId, blockData.withGlowBerry(true));
        }
    }

    private static final Set<org.bukkit.block.BlockFace> WATER_FACES = Set.of(
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST
    );

    private static final Predicate<Block> DEFAULT_SUGAR_CANE_PREDICATE = block -> {
        final Material material = block.getType();
        if (material == Material.SUGAR_CANE) return true;
        if (!Tag.SAND.isTagged(material) && !Tag.DIRT.isTagged(material)) {
            return false;
        }
        for (final BlockFace face : WATER_FACES) {
            final Material relative = block.getRelative(face).getType();
            if (relative == Material.WATER)
                return true;
        }
        return false;
    };
    private static final Predicate<Block> DEFAULT_KELP_PREDICATE = block ->
            (
                    block.getType().isSolid() ||
                            block.getType() == Material.KELP ||
                            block.getType() == Material.KELP_PLANT
            ) && (
                    block.getRelative(BlockFace.UP).getType() == Material.KELP ||
                            block.getRelative(BlockFace.UP).getType() == Material.KELP_PLANT ||
                            block.getRelative(BlockFace.UP).getType() == Material.WATER
            );

    private static final Predicate<Block> DEFAULT_TWISTING_VINES_PREDICATE = block ->
            block.getType().isSolid() ||
                    block.getType() == Material.TWISTING_VINES_PLANT ||
                    block.getType() == Material.TWISTING_VINES;
    private static final Predicate<Block> DEFAULT_WEEPING_VINES_PREDICATE = block ->
            block.getType().isSolid() ||
                    block.getType() == Material.WEEPING_VINES_PLANT ||
                    block.getType() == Material.WEEPING_VINES;

    private static final Set<BlockFace> DEFAULT_SUGAR_CANE_SUPPORTABLE_FACES = Set.of(BlockFace.DOWN);
    private static final Set<BlockFace> DEFAULT_KELP_SUPPORTABLE_FACES = Set.of(BlockFace.DOWN);

    private void loadAgeableSections(ConfigurationSection config) {
        final ConfigurationSection sugarCaneSection = config.getConfigurationSection(SUGAR_CANE_PATH);
        if (sugarCaneSection != null) {
            this.loadAgeableSection(
                    sugarCaneSection,
                    Material.SUGAR_CANE,
                    Sound.BLOCK_GRASS_PLACE,
                    DEFAULT_SUGAR_CANE_SUPPORTABLE_FACES,
                    m -> m == Material.SUGAR_CANE,
                    DEFAULT_SUGAR_CANE_PREDICATE,
                    Material.SUGAR_CANE,
                    Material.AIR
            );
        }
        final ConfigurationSection kelpSection = config.getConfigurationSection(KELP_PATH);
        if (kelpSection != null) {
            this.loadAgeableSection(
                    kelpSection,
                    Material.KELP,
                    Sound.BLOCK_GRASS_PLACE,
                    DEFAULT_KELP_SUPPORTABLE_FACES,
                    m -> m == Material.KELP_PLANT || m == Material.KELP,
                    DEFAULT_KELP_PREDICATE,
                    Material.KELP_PLANT,
                    Material.WATER
            );
        }
        final ConfigurationSection twistingVinesSection = config.getConfigurationSection(TWISTING_VINES_PATH);
        if (twistingVinesSection != null) {
            this.loadAgeableSection(
                    twistingVinesSection,
                    Material.TWISTING_VINES,
                    Sound.BLOCK_VINE_PLACE,
                    Set.of(BlockFace.DOWN),
                    m -> m == Material.TWISTING_VINES || m == Material.TWISTING_VINES_PLANT,
                    DEFAULT_TWISTING_VINES_PREDICATE,
                    Material.TWISTING_VINES_PLANT,
                    Material.AIR
            );
        }
        final ConfigurationSection weepingVinesSection = config.getConfigurationSection(WEEPING_VINES_PATH);
        if (weepingVinesSection != null) {
            this.loadAgeableSection(
                    weepingVinesSection,
                    Material.WEEPING_VINES,
                    Sound.BLOCK_WEEPING_VINES_PLACE,
                    Set.of(BlockFace.UP),
                    m -> m == Material.WEEPING_VINES || m == Material.WEEPING_VINES_PLANT,
                    DEFAULT_WEEPING_VINES_PREDICATE,
                    Material.WEEPING_VINES_PLANT,
                    Material.AIR
            );
        }
    }

    private void loadAgeableSection(
            ConfigurationSection config,
            Material ageableMaterial,
            Sound placeSound,
            Set<BlockFace> defaultSupportableFaces,
            Predicate<Material> worldTypeSamePredicate,
            Predicate<Block> defaultPlacePredicate,
            Material defaultLowerMaterial,
            Material breakReplacement
    ) {
        for (String itemId : config.getKeys(false)) {
            final Supplier<ItemStack> itemStackSupplier = this.loadItemStack(
                    config.getConfigurationSection(itemId),
                    itemId,
                    itemId
            );
            final int stateId = config.getInt(itemId + "." + STATE_ID_PATH);
            final int tippedStateId;
            if (config.contains(itemId + "." + TIPPED_STATE_ID_PATH)) {
                tippedStateId = config.getInt(itemId + "." + TIPPED_STATE_ID_PATH);
            } else {
                tippedStateId = stateId;
            }
            final String modelPath = config.getString(itemId + "." + MODEL_PATH_PATH);
            final int stackLimit = config.getInt(itemId + "." + STACK_LIMIT_PATH, Integer.MAX_VALUE);
            final var state = getAgeableById(ageableMaterial, stateId);
            final ConfigurationSection predicateSection = config.getConfigurationSection(itemId + "." + ALLOWED_SUPPORTABLE_BLOCKS);
            final Predicate<Block> placePredicate;
            if (predicateSection != null) {
                placePredicate = this.loadSupportableBlockPredicate(predicateSection);
            } else {
                placePredicate = defaultPlacePredicate;
            }
            final BlockDataSound blockDataSound = this.loadBlockDataSound(config.getConfigurationSection(itemId));
            final var data = BlockData.ageableData(
                    itemId,
                    ageableMaterial,
                    getAgeableById(ageableMaterial, stateId).getGlobalId(),
                    getAgeableById(ageableMaterial, tippedStateId).getGlobalId(),
                    modelPath,
                    placeSound,
                    worldTypeSamePredicate,
                    defaultLowerMaterial,
                    stackLimit,
                    breakReplacement,
                    this.loadSupportableFaces(config.getConfigurationSection(itemId), defaultSupportableFaces),
                    blockDataSound
            );
            this.blockSupportPredicateMap.put(itemId, placePredicate);
            this.blockDataMap.put(itemId, data);
            this.itemSupplierMap.put(itemId, itemStackSupplier);
            this.playerItemIds.add(itemId);
        }
    }

    private static final String TAGS_PATH = "tags";
    private static final String HOOK_BLOCK_IDS_PATH = "hook-block-ids";
    private static final String MATERIALS_PATH = "materials";

    private Predicate<Block> loadSupportableBlockPredicate(ConfigurationSection predicateSection) {
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
        return block -> {
            final Material material = block.getType();
            if (materialStrings.contains(material)) return true;
            for (Tag<Material> tag : tags) {
                if (tag.isTagged(material)) return true;
            }
            return hookBlockIds.contains(Hooks.getCustomBlockIdAt(block.getLocation()));
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
    private static final String INVENTORY_ITEM_PATH = "inventory-item";

    private Supplier<ItemStack> loadItemStack(ConfigurationSection section, String itemId, String hookId) {
        if (section.contains(INVENTORY_ITEM_PATH, true)) {
            final String actualHookId = section.getString(INVENTORY_ITEM_PATH);
            if (actualHookId != null) {
                return this.loadItemStack(section.getConfigurationSection(INVENTORY_ITEM_PATH), itemId, actualHookId);
            }
            return this.loadItemStack(section.getConfigurationSection(INVENTORY_ITEM_PATH), itemId, itemId);
        }
        final String materialStr = section.getString(MATERIAL_PATH);
        Material material;
        try {
            material = Material.valueOf(materialStr.toUpperCase());
        } catch (NullPointerException | IllegalArgumentException e) {
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
        if (hookId != null) {
            return () -> {
                final ItemStack hookItemStack = Hooks.getItem(hookId);
                return Objects.requireNonNullElse(hookItemStack, itemStack).clone();
            };
        }
        return itemStack::clone;
    }

    private Set<BlockFace> loadSupportableFaces(ConfigurationSection section, Set<BlockFace> defaultSupportableFaces) {
        if (!section.contains(SUPPORTABLE_FACES_PATH)) {
            return defaultSupportableFaces;
        }
        return section.getStringList(SUPPORTABLE_FACES_PATH)
                .stream()
                .map(s -> {
                    try {
                        return BlockFace.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static final String BLOCK_BREAK_MODIFIER_PATH = "block-break-modifier";
    private static final String HARDNESS_PATH = "hardness";
    private static final String TOOL_TYPES_PATH = "tool-types";
    private static final String REQUIRES_TOOL_TO_DROP_PATH = "requires-tool-to-drop";

    @Nullable
    private BlockBreakModifier loadBlockBreakModifier(ConfigurationSection section) {
        if (!section.contains(BLOCK_BREAK_MODIFIER_PATH)) {
            return null;
        }
        final ConfigurationSection blockBreakModifierSection = section.getConfigurationSection(BLOCK_BREAK_MODIFIER_PATH);
        if (blockBreakModifierSection == null) {
            return null;
        }
        final int hardness = blockBreakModifierSection.getInt(HARDNESS_PATH, 1);
        final boolean requiresToolToDrop = blockBreakModifierSection.getBoolean(REQUIRES_TOOL_TO_DROP_PATH, false);
        final Set<BlockBreakModifier.ToolType> toolTypes = blockBreakModifierSection.getStringList(TOOL_TYPES_PATH)
                .stream()
                .map(s -> {
                    try {
                        return BlockBreakModifier.ToolType.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        return new BlockBreakModifier(hardness, requiresToolToDrop, toolTypes);
    }

    private static final String STEP_SOUND_PATH = "step-sound";
    private static final String HIT_SOUND_PATH = "hit-sound";

    private BlockDataSound loadBlockDataSound(@Nullable ConfigurationSection section) {
        if (section == null) return null;
        final SoundData stepSound = this.loadSoundData(section.getConfigurationSection(STEP_SOUND_PATH));
        final SoundData hitSound = this.loadSoundData(section.getConfigurationSection(HIT_SOUND_PATH));
        return new BlockDataSound(stepSound, hitSound);
    }

    private static final String SOUND_NAME_PATH = "name";
    private static final String SOUND_CATEGORY_PATH = "category";
    private static final String SOUND_VOLUME_PATH = "volume";
    private static final String SOUND_PITCH_PATH = "pitch";

    private SoundData loadSoundData(@Nullable ConfigurationSection section) {
        if (section == null) return null;
        final String name = section.getString(SOUND_NAME_PATH);
        final SoundCategory category = SoundCategory.valueOf(section.getString(SOUND_CATEGORY_PATH).toUpperCase());
        final float volume = (float) section.getDouble(SOUND_VOLUME_PATH, 1);
        final float pitch = (float) section.getDouble(SOUND_PITCH_PATH, 1);
        return new SoundData(name, category, volume, pitch);
    }

}
