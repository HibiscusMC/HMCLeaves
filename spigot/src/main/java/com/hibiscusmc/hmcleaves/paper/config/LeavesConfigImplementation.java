package com.hibiscusmc.hmcleaves.paper.config;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.enums.Instrument;
import com.hibiscusmc.hmcleaves.common.block.BlockProperties;
import com.hibiscusmc.hmcleaves.common.block.LeavesBlock;
import com.hibiscusmc.hmcleaves.common.config.LeavesConfig;
import com.hibiscusmc.hmcleaves.paper.HMCLeavesPlugin;
import com.hibiscusmc.hmcleaves.paper.util.AdventureUtil;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class LeavesConfigImplementation implements LeavesConfig<BlockData> {

    public static final String DEBUG_ITEM_PERMISSION = "hmcleaves.debug";
    public static final String GIVE_ITEM_PERMISSION = "hmcleaves.give";
    public static final String PLACE_DECAYABLE_PERMISSION = "hmcleaves.placedecayable";
    public static final String DEBUG_ITEM_ID = "debug-item";

    private static final List<Material> LEAVES = Arrays.stream(Material.values()).filter(Tag.LEAVES::isTagged).toList();
    private static final List<Material> LOGS = Arrays.stream(Material.values()).filter(Tag.LOGS::isTagged).toList();

    private final HMCLeavesPlugin plugin;
    private final Path filePath;
    private final Map<String, LeavesBlock> blocksById;
    private final Map<Material, LeavesBlock> defaultBlocks;
    private final Map<String, Supplier<BlockData>> worldBlockData;
    private final Map<String, ItemStack> itemsById;
    private final Map<String, BlockDropConfig> blockDrops;

    private final NamespacedKey itemKey;
    private final NamespacedKey debugItemKey;
    private final NamespacedKey placingDecayableKey;

    private Collection<String> whitelistedWorlds;
    private boolean invertWhitelist;


    public LeavesConfigImplementation(
            HMCLeavesPlugin plugin,
            Path filePath,
            Map<String, LeavesBlock> blocksById,
            Map<Material, LeavesBlock> defaultBlocks,
            Map<String, Supplier<BlockData>> worldBlockData,
            Map<String, ItemStack> itemsById,
            Map<String, BlockDropConfig> blockDrops
    ) {
        this.plugin = plugin;
        this.filePath = filePath;
        this.blocksById = blocksById;
        this.worldBlockData = worldBlockData;
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
        this.itemsById.put(DEBUG_ITEM_ID, itemStack);
    }

    public boolean isDebugItem(ItemStack itemStack) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return false;
        }
        return itemMeta.getPersistentDataContainer().has(this.debugItemKey, PersistentDataType.BOOLEAN);
    }

    public void sendDebugInfo(Player player, LeavesBlock leavesBlock, BlockData blockData) {
        player.sendMessage(AdventureUtil.parse("<green>Debug Info:"));
        player.sendMessage(AdventureUtil.parse("<gray>Block ID: " + leavesBlock.id()));
        player.sendMessage(AdventureUtil.parse("<gray>Display Properties:"));
        for (var entry : leavesBlock.properties().properties().entrySet()) {
            player.sendMessage(AdventureUtil.parse("<gray>" + entry.getKey().id() + ": " + entry.getValue()));
        }
        player.sendMessage(AdventureUtil.parse("<green>Real Properties:"));
        player.sendMessage(AdventureUtil.parse("<gray>Material: " + blockData.getMaterial()));
        if (blockData instanceof Leaves leaves) {
            player.sendMessage(AdventureUtil.parse("<gray>Distance: " + leaves.getDistance()));
            player.sendMessage(AdventureUtil.parse("<gray>Persistent: " + leaves.isPersistent()));
            player.sendMessage(AdventureUtil.parse("<gray>Waterlogged: " + leaves.isWaterlogged()));
        }
    }

    @Override
    public @Nullable LeavesBlock getBlock(String id) {
        return this.blocksById.get(id);
    }

    public @Nullable ItemStack createItemStack(String id) {
        final ItemStack itemStack = this.itemsById.get(id);
        if (itemStack == null) {
            return null;
        }
        return itemStack.clone();
    }

    public @Nullable LeavesBlock getBlockFromItem(ItemStack itemStack) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return null;
        }
        final String blockId = itemMeta.getPersistentDataContainer().get(this.itemKey, PersistentDataType.STRING);
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

    public @Nullable BlockData getWorldBlockData(String id) {
        final Supplier<BlockData> blockDataSupplier = this.worldBlockData.get(id);
        if (blockDataSupplier == null) {
            return null;
        }
        return blockDataSupplier.get();
    }

    @Override
    public void load() {
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(this.filePath.toFile());
        final ConfigurationSection leavesSection = config.getConfigurationSection("leaves");
        this.registerDebugItem();
        if (leavesSection != null) {
            this.loadLeavesBlocks(leavesSection);
        } else {
            this.plugin.getLogger().warning("'leaves' section does not exist in " + this.filePath.getFileName());
        }
        final ConfigurationSection logsSection = config.getConfigurationSection("logs");
        if (logsSection != null) {
            this.loadLogsBlocks(logsSection);
        } else {
            this.plugin.getLogger().warning("'logs' section does not exist in " + this.filePath.getFileName());
        }
        this.loadDefaults();
        this.loadWhitelistedWorlds(config);
    }

    private void loadLeavesBlocks(ConfigurationSection config) {
        for (String id : config.getKeys(false)) {
            final int distance = config.getInt(id + ".distance");
            final boolean persistent = config.getBoolean(id + ".persistent");
            final String materialString = config.getString(id + ".material");
            if (materialString == null) {
                this.plugin.getLogger().warning("Invalid material for leaves block: " + id);
                continue;
            }
            final Material material = Material.matchMaterial(materialString);
            if (material == null) {
                this.plugin.getLogger().warning("Invalid material for leaves block: " + id);
                continue;
            }
            final BlockProperties properties = BlockProperties.builder()
                    .addProperty(BlockProperties.DISTANCE, distance)
                    .addProperty(BlockProperties.PERSISTENT, persistent)
                    .build();
            final WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData(material.createBlockData());
            state.setDistance(distance);
            state.setPersistent(persistent);
            this.blocksById.put(id, new LeavesBlock(id, properties, state::clone));
            this.worldBlockData.put(id, material::createBlockData);
            final ConfigurationSection itemStackSection = config.getConfigurationSection(id + ".item");
            if (itemStackSection != null) {
                this.loadItemStack(id, itemStackSection);
            }
            final ConfigurationSection dropsSection = config.getConfigurationSection(id + ".drops");
            if (dropsSection != null) {
                this.loadDrops(id, dropsSection);
            }
        }
    }

    private void loadLogsBlocks(ConfigurationSection config) {
        for (String id : config.getKeys(false)) {
            final String instrumentString = config.getString(id + ".instrument", "");
            final int note = config.getInt(id + ".note", 0);
            final boolean powered = config.getBoolean(id + ".powered");
            final Instrument instrument;
            try {
                instrument = Instrument.valueOf(instrumentString);
            } catch (IllegalArgumentException e) {
                this.plugin.getLogger().severe("Invalid instrument " + instrumentString + " for log block " + id);
                return;
            }
            final String logMaterialString = config.getString(id + ".log-material");
            if (logMaterialString == null) {
                this.plugin.getLogger().warning("Invalid material for log block: " + id);
                continue;
            }
            final Material logMaterial = Material.matchMaterial(logMaterialString);
            if (logMaterial == null) {
                this.plugin.getLogger().warning("Invalid material for log block: " + id);
                continue;
            }
            final Material material = Material.NOTE_BLOCK;
            final BlockProperties properties = BlockProperties.builder()
                    .addProperty(BlockProperties.INSTRUMENT, instrument)
                    .addProperty(BlockProperties.NOTE, note)
                    .addProperty(BlockProperties.POWERED, powered)
                    .build();
            final WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData(material.createBlockData());
            state.setInstrument(instrument);
            state.setNote(note);
            state.setPowered(powered);
            this.blocksById.put(id, new LeavesBlock(id, properties, state::clone));
            this.worldBlockData.put(id, logMaterial::createBlockData);
            final ConfigurationSection itemStackSection = config.getConfigurationSection(id + ".item");
            if (itemStackSection != null) {
                this.loadItemStack(id, itemStackSection);
            }
            final ConfigurationSection dropsSection = config.getConfigurationSection(id + ".drops");
            if (dropsSection != null) {
                this.loadDrops(id, dropsSection);
            }
        }
    }

    private void loadDefaults() {
        for (Material material : LEAVES) {
            final BlockProperties properties = BlockProperties.builder()
                    .addProperty(BlockProperties.DISTANCE, 7)
                    .addProperty(BlockProperties.PERSISTENT, false)
                    .build();
            final WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData(material.createBlockData());
            final LeavesBlock block = new LeavesBlock(material.toString().toLowerCase(), properties, state::clone);
            state.setDistance(7);
            state.setPersistent(false);
            this.defaultBlocks.put(material, block);
            this.blocksById.put(block.id(), block);
            this.worldBlockData.put(block.id(), material::createBlockData);
            this.itemsById.put(block.id(), new ItemStack(material));
        }
        for (Material material : LOGS) {
            final BlockProperties properties = BlockProperties.builder()
                    .addProperty(BlockProperties.DISTANCE, 7)
                    .addProperty(BlockProperties.PERSISTENT, false)
                    .build();
            final WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData(material.createBlockData());
            final LeavesBlock block = new LeavesBlock(material.toString().toLowerCase(), properties, state::clone);
            this.defaultBlocks.put(material, block);
            this.blocksById.put(block.id(), block);
            this.worldBlockData.put(block.id(), material::createBlockData);
            this.itemsById.put(block.id(), new ItemStack(material));
        }
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
        this.itemsById.put(id, itemStack);
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
        ItemStack sapling = null;
        if (saplingSection != null) {
            sapling = this.loadItemStack(saplingSection);
        }
        final boolean dropsSelf = config.getBoolean("drops-self", true);
        final ItemStack itemStack = this.itemsById.get(id);
        if (itemStack == null && dropsSelf) {
            this.plugin.getLogger().warning("Invalid item stack for item: " + id);
            return;
        }
        final BlockDropConfig drops = new BlockDropConfig(requiresShears, sapling == null ? null : sapling.clone(), itemStack == null ? null : itemStack.clone());
        this.blockDrops.put(id, drops);
    }

    private void loadWhitelistedWorlds(ConfigurationSection config) {
        this.whitelistedWorlds = config.getStringList("whitelisted-worlds");
        this.invertWhitelist = config.getBoolean("invert-whitelist", false);
    }

    @Override
    public boolean isWorldWhitelisted(String worldName) {
        if (this.invertWhitelist) {
            return !this.whitelistedWorlds.contains(worldName);
        }
        return this.whitelistedWorlds.contains(worldName);
    }

    @Override
    public boolean isWorldWhitelisted(UUID worldId) {
        final World world = this.plugin.getServer().getWorld(worldId);
        if (world == null) {
            return false;
        }
        return this.isWorldWhitelisted(world.getName());
    }

    @Override
    public @Nullable LeavesBlock getLeavesBlockFromWorldBlockData(BlockData blockData) {
        return this.defaultBlocks.get(blockData.getMaterial());
    }

    @Override
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
