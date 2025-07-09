package com.hibiscusmc.hmcleaves.paper.config;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.enums.Instrument;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlock;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlockState;
import com.hibiscusmc.hmcleaves.paper.block.LeavesBlock;
import com.hibiscusmc.hmcleaves.paper.block.LogBlock;
import com.hibiscusmc.hmcleaves.paper.util.AdventureUtil;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LeavesConfig {

    public static final String DEBUG_ITEM_PERMISSION = "hmcleaves.debug";
    public static final String GIVE_ITEM_PERMISSION = "hmcleaves.give";
    public static final String PLACE_DECAYABLE_PERMISSION = "hmcleaves.placedecayable";

    public static final String DEBUG_ITEM_ID = "debug-item";

    private static final List<Material> LEAVES = Arrays.stream(Material.values()).filter(Tag.LEAVES::isTagged).toList();

    private final HMCLeaves plugin;
    private final Path filePath;
    private final Map<String, CustomBlock> blocksById;
    private final Map<Material, CustomBlock> defaultBlocks;
    private final Map<String, Supplier<ItemStack>> itemsById;
    private final Map<String, BlockDropConfig> blockDrops;

    private final NamespacedKey itemKey;
    private final NamespacedKey debugItemKey;
    private final NamespacedKey placingDecayableKey;

    private Collection<String> whitelistedWorlds;
    private boolean invertWhitelist;

    private boolean debugMode;

    private boolean handleMining;

    public LeavesConfig(
            HMCLeaves plugin,
            Path filePath,
            Map<String, CustomBlock> blocksById,
            Map<Material, CustomBlock> defaultBlocks,
            Map<String, Supplier<ItemStack>> itemsById,
            Map<String, BlockDropConfig> blockDrops
    ) {
        this.plugin = plugin;
        this.filePath = filePath;
        this.blocksById = blocksById;
        this.defaultBlocks = defaultBlocks;
        this.itemsById = itemsById;
        this.blockDrops = blockDrops;

        this.itemKey = new NamespacedKey(this.plugin, "item");
        this.debugItemKey = new NamespacedKey(this.plugin, "debug-item");
        this.placingDecayableKey = new NamespacedKey(this.plugin, "placing-decayable");
    }

    private void registerDebugItem() {
        final ItemStack itemStack = new ItemStack(Material.STICK);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.getPersistentDataContainer().set(this.debugItemKey, PersistentDataType.BOOLEAN, true);
        itemMeta.displayName(AdventureUtil.MINI_MESSAGE.deserialize("<green>Debug Item"));
        itemMeta.lore(List.of(
                AdventureUtil.MINI_MESSAGE.deserialize("<gray>Right click to get the block data")
        ));
        itemStack.setItemMeta(itemMeta);
        this.itemsById.put(DEBUG_ITEM_ID, itemStack::clone);
    }

    public boolean isDebugItem(ItemStack itemStack) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return false;
        }
        return itemMeta.getPersistentDataContainer().has(this.debugItemKey, PersistentDataType.BOOLEAN);
    }

    public void sendDebugInfo(Player player, CustomBlockState customBlockState, BlockData blockData) {
        final CustomBlock customBlock = customBlockState.customBlock();
        player.sendMessage(Component.text("Debug Info:").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Block ID: " + customBlock.id()).color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Display Properties:").color(NamedTextColor.GRAY));
        for (var entry : customBlockState.getPropertiesByName().entrySet()) {
            player.sendMessage(Component.text(entry.getKey() + ": " + entry.getValue()).color(NamedTextColor.GRAY));
        }
        player.sendMessage(Component.text("Real Properties:").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Material: " + blockData.getMaterial()).color(NamedTextColor.GRAY));
        if (blockData instanceof Leaves leaves) {
            player.sendMessage(Component.text("Distance: " + leaves.getDistance()).color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("Persistent: " + leaves.isPersistent()).color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("Waterlogged: " + leaves.isWaterlogged()).color(NamedTextColor.GRAY));
        }
        if (blockData instanceof Orientable orientable) {
            player.sendMessage(Component.text("Axis: " + orientable.getAxis()).color(NamedTextColor.GRAY));
        }
    }

    public @Nullable CustomBlock getBlock(String id) {
        return this.blocksById.get(id);
    }

    public @Nullable ItemStack createItemStack(String id) {
        final Supplier<ItemStack> itemStackSupplier = this.itemsById.get(id);
        if (itemStackSupplier == null) {
            return null;
        }
        return itemStackSupplier.get();
    }

    public @Nullable CustomBlock getBlockFromItem(ItemStack itemStack) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return null;
        }
        String blockId = this.plugin.itemHook().getItemId(itemStack);
        if (blockId == null) {
            blockId = itemMeta.getPersistentDataContainer().get(this.itemKey, PersistentDataType.STRING);
        }
        final Material material = itemStack.getType();
        if (blockId == null) {
            return this.defaultBlocks.get(material);
        }
        return this.blocksById.getOrDefault(blockId, this.defaultBlocks.get(material));
    }

    public BlockDropConfig getBlockDrops(String id) {
        return this.blockDrops.get(id);
    }

    public Collection<String> itemIds() {
        return this.itemsById.keySet();
    }

    public boolean isItemId(String itemId) {
        return this.itemsById.containsKey(itemId);
    }

    public boolean debugMode() {
        return this.debugMode;
    }

    public boolean handleMining() {
        return this.handleMining;
    }

    public void load() {
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(this.filePath.toFile());
        this.debugMode = config.getBoolean("debug-mode", false);
        this.handleMining = config.getBoolean("handle-mining", true);
        final ConfigurationSection blocksSection = config.getConfigurationSection("blocks");
        this.registerDebugItem();
        if (blocksSection != null) {
            this.loadCustomBlocks(blocksSection);
        } else {
            this.plugin.getLogger().warning("'leaves' section does not exist in " + this.filePath.getFileName());
        }
        this.loadDefaults();
        this.loadWhitelistedWorlds(config);
    }

    private void loadCustomBlocks(ConfigurationSection config) {
        for (String id : config.getKeys(false)) {
            final ConfigurationSection blockSection = config.getConfigurationSection(id);
            if (blockSection == null) {
                this.plugin.getLogger().warning("Invalid configuration for custom block: " + id);
                continue;
            }
            this.loadCustomBlock(id, blockSection);
        }
    }

    private void loadDefaults() {
        for (Material material : LEAVES) {
            final WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData(material.createBlockData());
            final CustomBlock block = new LeavesBlock(material.toString().toLowerCase(), material, state);
            state.setDistance(7);
            state.setPersistent(false);
            this.defaultBlocks.put(material, block);
            this.blocksById.put(block.id(), block);
            this.blockDrops.put(block.id(), new BlockDropConfig(false, () -> null, () -> new ItemStack(material)));
            this.registerItem(block.id(), new ItemStack(material));
        }
    }

    private void loadCustomBlock(String id, ConfigurationSection section) {
        final String type = section.getString("type");
        if (type == null) {
            this.plugin.getLogger().warning("Invalid type for custom block: " + id);
            return;
        }
        switch (type.toLowerCase()) {
            case "leaves" -> this.loadLeavesBlock(id, section);
            case "log" -> this.loadLogBlock(id, section);
            default -> this.plugin.getLogger().warning("Unknown custom block type: " + type + " for block: " + id);
        }
    }

    private void loadLeavesBlock(String id, ConfigurationSection section) {
        final int distance = section.getInt("distance");
        final boolean persistent = section.getBoolean("persistent");
        final String materialString = section.getString("material");
        if (materialString == null) {
            this.plugin.getLogger().warning("Invalid material for leaves block: " + id);
            return;
        }
        final Material material = Material.matchMaterial(materialString);
        if (material == null || !Tag.LEAVES.isTagged(material)) {
            this.plugin.getLogger().warning("Invalid material for leaves block: " + id);
            return;
        }
        final WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData(material.createBlockData());
        state.setDistance(distance);
        state.setPersistent(persistent);
        final LeavesBlock leavesBlock = new LeavesBlock(id, material, state);
        this.blocksById.put(id, leavesBlock);
        final ConfigurationSection itemStackSection = section.getConfigurationSection("item");
        if (itemStackSection != null) {
            this.loadItemStack(id, itemStackSection);
        }
        final ConfigurationSection dropsSection = section.getConfigurationSection("drops");
        if (dropsSection != null) {
            this.loadDrops(id, dropsSection);
        }
    }

    private void loadLogBlock(String id, ConfigurationSection section) {
        final String logMaterialString = section.getString("log-material");
        if (logMaterialString == null) {
            this.plugin.getLogger().warning("Invalid material for log block: " + id);
            return;
        }
        final Material logMaterial = Material.matchMaterial(logMaterialString);
        if (logMaterial == null || !Tag.LOGS.isTagged(logMaterial)) {
            this.plugin.getLogger().warning("Invalid material for log block: " + id);
            return;
        }
        final ConfigurationSection orientationsSection = section.getConfigurationSection("orientations");
        final ConfigurationSection strippedOrientationsSection = section.getConfigurationSection("stripped-orientations");
        final Map<Axis, WrappedBlockState> axisStates;
        if (orientationsSection == null) {
            axisStates = new HashMap<>();
        } else {
            axisStates = this.loadLogBlockStates(orientationsSection);
        }
        final Map<Axis, WrappedBlockState> strippedAxisStates;
        if (strippedOrientationsSection == null) {
            strippedAxisStates = new HashMap<>();
        } else {
            strippedAxisStates = this.loadLogBlockStates(strippedOrientationsSection);
        }
        this.blocksById.put(id, new LogBlock(id, logMaterial, axisStates, strippedAxisStates));
        final ConfigurationSection itemStackSection = section.getConfigurationSection("item");
        if (itemStackSection != null) {
            this.loadItemStack(id, itemStackSection);
        }
        final ConfigurationSection dropsSection = section.getConfigurationSection("drops");
        if (dropsSection != null) {
            this.loadDrops(id, dropsSection);
        }
    }

    private Map<Axis, WrappedBlockState> loadLogBlockStates(ConfigurationSection section) {
        final Map<Axis, WrappedBlockState> axisStates = new HashMap<>();
        for (String axisString : section.getKeys(false)) {
            final Axis axis = Axis.valueOf(axisString.toUpperCase());
            final ConfigurationSection axisSection = section.getConfigurationSection(axisString);
            if (axisSection == null) {
                this.plugin.getLogger().warning("Invalid configuration for axis: " + axisString + " in log block");
                continue;
            }
            final WrappedBlockState axisState = this.loadLogBlockState(axisString, axisSection);
            if (axisState != null) {
                axisStates.put(axis, axisState);
            }
        }
        return axisStates;
    }

    private @Nullable WrappedBlockState loadLogBlockState(String id, ConfigurationSection section) {
        final String instrumentString = section.getString("instrument", "");
        final int note = section.getInt("note", 0);
        final boolean powered = section.getBoolean("powered");
        final Instrument instrument;
        try {
            instrument = Instrument.valueOf(instrumentString);
        } catch (IllegalArgumentException e) {
            this.plugin.getLogger().severe("Invalid instrument " + instrumentString + " for log block " + id);
            return null;
        }
        final WrappedBlockState state = StateTypes.NOTE_BLOCK.createBlockState();
        state.setInstrument(instrument);
        state.setNote(note);
        state.setPowered(powered);
        return state;
    }

    private void loadItemStack(@Nullable String id, ConfigurationSection section) {
        final ItemStack itemStack = this.loadItemStack(section);
        if (itemStack == null) {
            this.plugin.getLogger().warning("Invalid item stack for item: " + id);
            return;
        }
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            this.plugin.getLogger().warning("Invalid item stack for item: " + id);
            return;
        }
        if (id != null) {
            itemMeta.getPersistentDataContainer().set(this.itemKey, PersistentDataType.STRING, id);
        }
        itemStack.setItemMeta(itemMeta);
        this.registerItem(id, itemStack);
    }

    private @Nullable ItemStack loadItemStack(ConfigurationSection section) {
        final Material material = Material.matchMaterial(section.getString("material", ""));
        if (material == null) {
            this.plugin.getLogger().warning("Invalid material for item stack: " + section.getString("material") + " in section " + section.getName());
            return null;
        }
        final int amount = section.getInt("amount", 1);
        final ItemStack itemStack = new ItemStack(material, amount);
        final boolean glowing = section.getBoolean("glowing", false);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (glowing) {
            itemMeta.setEnchantmentGlintOverride(true);
        }
        final String nameString = section.getString("name", "");
        if (!nameString.isBlank()) {
            itemMeta.displayName(AdventureUtil.MINI_MESSAGE.deserialize(nameString));
        }
        final List<Component> lore = section.getStringList("lore")
                .stream()
                .map(AdventureUtil.MINI_MESSAGE::deserialize)
                .collect(Collectors.toList());
        itemMeta.lore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private void loadDrops(String id, ConfigurationSection config) {
        final boolean requiresShears = config.getBoolean("requires-shears", false);
        final ConfigurationSection saplingSection = config.getConfigurationSection("sapling");
        final ItemStack sapling = saplingSection == null ? null : this.loadItemStack(saplingSection);
        final boolean dropsSelf = config.getBoolean("drops-self", true);
        final Supplier<ItemStack> itemStackSupplier = this.itemsById.get(id);
        if (itemStackSupplier == null && dropsSelf) {
            this.plugin.getLogger().warning("Invalid item stack for item: " + id);
            return;
        }
        final BlockDropConfig drops = new BlockDropConfig(requiresShears, () -> sapling, itemStackSupplier == null ? () -> null : itemStackSupplier);
        this.blockDrops.put(id, drops);
    }

    private void registerItem(String id, ItemStack itemStack) {
        this.itemsById.put(id, () -> {
            final ItemStack hookItem = this.plugin.itemHook().fromId(id);
            if (hookItem != null) {
                this.plugin.log(Level.INFO, "Nexo Item Id: " + id);
                return hookItem;
            }
            this.plugin.log(Level.INFO, "Normal Item Id: " + id);
            return itemStack.clone();
        });
    }

    private void loadWhitelistedWorlds(ConfigurationSection config) {
        this.whitelistedWorlds = config.getStringList("whitelisted-worlds");
        this.invertWhitelist = config.getBoolean("invert-whitelist", false);
    }

    public boolean isWorldWhitelisted(String worldName) {
        if (this.invertWhitelist) {
            return !this.whitelistedWorlds.contains(worldName);
        }
        return this.whitelistedWorlds.contains(worldName);
    }

    public boolean isWorldWhitelisted(UUID worldId) {
        final World world = this.plugin.getServer().getWorld(worldId);
        if (world == null) {
            return false;
        }
        return this.isWorldWhitelisted(world.getName());
    }

    public @Nullable CustomBlock getCustomBlockFromWorldBlockData(BlockData blockData) {
        return this.defaultBlocks.get(blockData.getMaterial());
    }

    public int chunkScanVersion() {
        return 0;
    }

    public NamespacedKey itemKey() {
        return this.itemKey;
    }

    public NamespacedKey debugItemKey() {
        return this.debugItemKey;
    }

    public NamespacedKey placingDecayableKey() {
        return this.placingDecayableKey;
    }

    public boolean isPlacingDecayable(Player player) {
        final Boolean placingDecayable = player.getPersistentDataContainer().get(this.placingDecayableKey, PersistentDataType.BOOLEAN);
        return Objects.requireNonNullElse(placingDecayable, false);
    }

    public void setPlacingDecayable(Player player, boolean placingDecayable) {
        player.getPersistentDataContainer().set(this.placingDecayableKey, PersistentDataType.BOOLEAN, placingDecayable);
    }

}
