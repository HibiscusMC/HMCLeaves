package io.github.fisher2911.hmcleaves.nms;

import com.jeff_media.customblockdata.CustomBlockData;
import com.mojang.serialization.Lifecycle;
import io.github.fisher2911.hmcleaves.util.PositionUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_19_R1.util.CraftMagicNumbers;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "unchecked"})
public class LeafHandler_1_19 implements LeafHandler {

    private static final CustomLeafInfo JUNGLE_LEAVES = new CustomLeafInfo(
            BlockBehaviour.Properties.copy(Blocks.JUNGLE_LEAVES),
            Blocks.JUNGLE_LEAVES,
            Items.JUNGLE_LEAVES,
            "minecraft:block:minecraft:jungle_leaves",
            List.of("m", "o", "p", "v"),
            Material.JUNGLE_LEAVES
    );
    private static final CustomLeafInfo MANGROVE_LEAVES = new CustomLeafInfo(
            BlockBehaviour.Properties.copy(Blocks.MANGROVE_LEAVES),
            Blocks.MANGROVE_LEAVES,
            Items.MANGROVE_LEAVES,
            "minecraft:block:minecraft:mangrove_leaves",
            List.of("x", "y"),
            Material.MANGROVE_LEAVES,
            false,
            true
    );
    private static final CustomLeafInfo ACACIA_LEAVES = new CustomLeafInfo(
            BlockBehaviour.Properties.copy(Blocks.ACACIA_LEAVES),
            Blocks.ACACIA_LEAVES,
            Items.ACACIA_LEAVES,
            "minecraft:block:minecraft:acacia_leaves",
            List.of("j"),
            Material.ACACIA_LEAVES
    );
    private static final CustomLeafInfo DARK_OAK_LEAVES = new CustomLeafInfo(
            BlockBehaviour.Properties.copy(Blocks.DARK_OAK_LEAVES),
            Blocks.DARK_OAK_LEAVES,
            Items.DARK_OAK_LEAVES,
            "minecraft:block:minecraft:dark_oak_leaves",
            List.of("h"),
            Material.DARK_OAK_LEAVES
    );
    private static final CustomLeafInfo OAK_LEAVES = new CustomLeafInfo(
            BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES),
            Blocks.OAK_LEAVES,
            Items.OAK_LEAVES,
            "minecraft:block:minecraft:oak_leaves",
            List.of("g", "n", "u", "z", "A", "B", "F", "G", "H", "I"),
            Material.OAK_LEAVES
    );
    private static final CustomLeafInfo SPRUCE_LEAVES = new CustomLeafInfo(
            BlockBehaviour.Properties.copy(Blocks.SPRUCE_LEAVES),
            Blocks.SPRUCE_LEAVES,
            Items.SPRUCE_LEAVES,
            "minecraft:block:minecraft:spruce_leaves",
            List.of("k", "l", "q", "r"),
            Material.SPRUCE_LEAVES
    );
    private static final CustomLeafInfo FLOWERING_AZALEA_LEAVES = new CustomLeafInfo(
            BlockBehaviour.Properties.copy(Blocks.FLOWERING_AZALEA_LEAVES),
            Blocks.FLOWERING_AZALEA_LEAVES,
            Items.FLOWERING_AZALEA_LEAVES,
            "minecraft:block:minecraft:flowering_azalea_leaves",
            List.of("w"),
            Material.FLOWERING_AZALEA_LEAVES,
            true,
            false
    );
    private static final CustomLeafInfo AZALEA_LEAVES = new CustomLeafInfo(
            BlockBehaviour.Properties.copy(Blocks.AZALEA_LEAVES),
            Blocks.AZALEA_LEAVES,
            Items.AZALEA_LEAVES,
            "minecraft:block:minecraft:azalea_leaves",
            List.of("w"),
            Material.AZALEA_LEAVES,
            true,
            false
    );
    private static final CustomLeafInfo BIRCH_LEAVES = new CustomLeafInfo(
            BlockBehaviour.Properties.copy(Blocks.BIRCH_LEAVES),
            Blocks.BIRCH_LEAVES,
            Items.BIRCH_LEAVES,
            "minecraft:block:minecraft:birch_leaves",
            List.of("i", "s", "t", "C", "D", "E"),
            Material.BIRCH_LEAVES
    );
    private static final List<CustomLeafInfo> CUSTOM_LEAF_INFOS = List.of(
            JUNGLE_LEAVES,
            MANGROVE_LEAVES,
            ACACIA_LEAVES,
            DARK_OAK_LEAVES,
            OAK_LEAVES,
            SPRUCE_LEAVES,
            FLOWERING_AZALEA_LEAVES,
            AZALEA_LEAVES,
            BIRCH_LEAVES
    );

    private final Supplier<JavaPlugin> pluginSupplier;
    private JavaPlugin plugin;
    private final Map<CustomLeafInfo, LeavesBlock> leafInfoToLeavesBlockMap = new HashMap<>();
    private final LeafDataSupplier leafDataSupplier;
    private final PDCHelper pdcHelper;

    public LeafHandler_1_19(Supplier<JavaPlugin> pluginSupplier, LeafDataSupplier leafDataSupplier, PDCHelper pdcHelper) {
        this.pluginSupplier = pluginSupplier;
        this.leafDataSupplier = leafDataSupplier;
        this.pdcHelper = pdcHelper;
        LeavesBlock.leafDataSupplier = leafDataSupplier;
        LeavesBlock.pdcHelper = pdcHelper;
    }

// minecraft:item:minecraft:spruce_leaves ResourceKey[minecraft:item / minecraft:spruce_leaves]
// minecraft:worldgen/tree_decorator_type:minecraft:leave_vine ResourceKey[minecraft:worldgen/tree_decorator_type / minecraft:leave_vine]
// minecraft:item:minecraft:jungle_leaves ResourceKey[minecraft:item / minecraft:jungle_leaves]
// minecraft:item:minecraft:azalea_leaves ResourceKey[minecraft:item / minecraft:azalea_leaves]
// minecraft:sound_event:minecraft:block.azalea_leaves.fall ResourceKey[minecraft:sound_event / minecraft:block.azalea_leaves.fall]
// minecraft:worldgen/tree_decorator_type:minecraft:attached_to_leaves ResourceKey[minecraft:worldgen/tree_decorator_type / minecraft:attached_to_leaves]
// minecraft:worldgen/configured_feature:minecraft:clay_with_dripleaves ResourceKey[minecraft:worldgen/configured_feature / minecraft:clay_with_dripleaves]
// minecraft:item:minecraft:mangrove_leaves ResourceKey[minecraft:item / minecraft:mangrove_leaves]
// minecraft:block:minecraft:jungle_leaves ResourceKey[minecraft:block / minecraft:jungle_leaves]
// minecraft:sound_event:minecraft:block.azalea_leaves.hit ResourceKey[minecraft:sound_event / minecraft:block.azalea_leaves.hit]
// minecraft:item:minecraft:oak_leaves ResourceKey[minecraft:item / minecraft:oak_leaves]
// minecraft:block:minecraft:mangrove_leaves ResourceKey[minecraft:block / minecraft:mangrove_leaves]
// minecraft:block:minecraft:acacia_leaves ResourceKey[minecraft:block / minecraft:acacia_leaves]
// minecraft:sound_event:minecraft:block.azalea_leaves.break ResourceKey[minecraft:sound_event / minecraft:block.azalea_leaves.break]
// minecraft:sound_event:minecraft:block.azalea_leaves.place ResourceKey[minecraft:sound_event / minecraft:block.azalea_leaves.place]
// minecraft:block:minecraft:dark_oak_leaves ResourceKey[minecraft:block / minecraft:dark_oak_leaves]
// minecraft:worldgen/configured_feature:minecraft:clay_pool_with_dripleaves ResourceKey[minecraft:worldgen/configured_feature / minecraft:clay_pool_with_dripleaves]
// minecraft:item:minecraft:flowering_azalea_leaves ResourceKey[minecraft:item / minecraft:flowering_azalea_leaves]
// minecraft:item:minecraft:acacia_leaves ResourceKey[minecraft:item / minecraft:acacia_leaves]
// minecraft:block:minecraft:oak_leaves ResourceKey[minecraft:block / minecraft:oak_leaves]
// minecraft:block:minecraft:spruce_leaves ResourceKey[minecraft:block / minecraft:spruce_leaves]
// minecraft:sound_event:minecraft:block.azalea_leaves.step ResourceKey[minecraft:sound_event / minecraft:block.azalea_leaves.step]
// minecraft:block:minecraft:flowering_azalea_leaves ResourceKey[minecraft:block / minecraft:flowering_azalea_leaves]
// minecraft:custom_stat:minecraft:leave_game ResourceKey[minecraft:custom_stat / minecraft:leave_game]
// minecraft:block:minecraft:azalea_leaves ResourceKey[minecraft:block / minecraft:azalea_leaves]
// minecraft:item:minecraft:dark_oak_leaves ResourceKey[minecraft:item / minecraft:dark_oak_leaves]
// minecraft:item:minecraft:birch_leaves ResourceKey[minecraft:item / minecraft:birch_leaves]
// minecraft:block:minecraft:birch_leaves ResourceKey[minecraft:block / minecraft:birch_leaves]

// Field: b - minecraft:crimson_fungus_planted
// Field: c - minecraft:warped_fungus
// Field: d - minecraft:warped_fungus_planted
// Field: e - minecraft:huge_brown_mushroom
// Field: f - minecraft:huge_red_mushroom
// Field: g - minecraft:oak
// Field: h - minecraft:dark_oak
// Field: i - minecraft:birch
// Field: j - minecraft:acacia
// Field: k - minecraft:spruce
// Field: l - minecraft:pine
// Field: m - minecraft:jungle_tree
// Field: n - minecraft:fancy_oak
// Field: o - minecraft:jungle_tree_no_vine
// Field: p - minecraft:mega_jungle_tree
// Field: q - minecraft:mega_spruce
// Field: r - minecraft:mega_pine
// Field: s - minecraft:super_birch_bees_0002
// Field: t - minecraft:super_birch_bees
// Field: u - minecraft:swamp_oak
// Field: v - minecraft:jungle_bush
// Field: w - minecraft:azalea_tree
// Field: x - minecraft:mangrove
// Field: y - minecraft:tall_mangrove
// Field: z - minecraft:oak_bees_0002
// Field: A - minecraft:oak_bees_002
// Field: B - minecraft:oak_bees_005
// Field: C - minecraft:birch_bees_0002
// Field: D - minecraft:birch_bees_002
// Field: E - minecraft:birch_bees_005
// Field: F - minecraft:fancy_oak_bees_0002
// Field: G - minecraft:fancy_oak_bees_002
// Field: H - minecraft:fancy_oak_bees_005
// Field: I - minecraft:fancy_oak_bees

    private static final Field intrusiveHolderCacheField;
    private static final Field frozenField;
    private static final Field byIdField;
    private static final Field toIdField;
    private static final Field blockField;
    private static final Field valuesField;
    private static final Field byKeyField;
    private static final Field blockTagsField;
    private static final Field tToIdField;
    private static final Field idToTField;
    private static final Field foliageProviderField;
    private static final Field blockMaterialField;
    private static final Method setFlammableMethod;


    static {
        try {

            intrusiveHolderCacheField = MappedRegistry.class.getDeclaredField("cc");
            intrusiveHolderCacheField.setAccessible(true);

            frozenField = MappedRegistry.class.getDeclaredField("ca");
            frozenField.setAccessible(true);

            byIdField = MappedRegistry.class.getDeclaredField("bS");
            byIdField.setAccessible(true);

            toIdField = MappedRegistry.class.getDeclaredField("bT");
            toIdField.setAccessible(true);

            blockField = BlockItem.class.getDeclaredField("c");
            blockField.setAccessible(true);

            valuesField = Registry.BLOCK_REGISTRY.getClass().getDeclaredField("a");
            valuesField.setAccessible(true);

            byKeyField = MappedRegistry.class.getDeclaredField("bV");
            byKeyField.setAccessible(true);

            blockTagsField = Holder.Reference.class.getDeclaredField("b");
            blockTagsField.setAccessible(true);

            tToIdField = Block.BLOCK_STATE_REGISTRY.getClass().getDeclaredField("c");
            tToIdField.setAccessible(true);

            idToTField = Block.BLOCK_STATE_REGISTRY.getClass().getDeclaredField("d");
            idToTField.setAccessible(true);

            foliageProviderField = TreeConfiguration.class.getDeclaredField("e");
            foliageProviderField.setAccessible(true);

            blockMaterialField = CraftMagicNumbers.class.getDeclaredField("BLOCK_MATERIAL");
            blockMaterialField.setAccessible(true);

            final FireBlock blockfire = (FireBlock) Blocks.FIRE;

            // make our leaf flammable again
            setFlammableMethod = blockfire.getClass().getDeclaredMethod("a", Block.class, int.class, int.class);
            setFlammableMethod.setAccessible(true);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize");
        }
    }

    @Override
    public void load() {
        for (CustomLeafInfo customLeafInfo : CUSTOM_LEAF_INFOS) {
            load(customLeafInfo);
        }
        loadAzaleaTreeLeaves();
    }

    public final Map<Block, Block> previousToNew = new HashMap<>();

    private void load(CustomLeafInfo customLeafInfo) {
        try {

            // make customBlock and item registries accessible
            intrusiveHolderCacheField.set(Registry.BLOCK, new IdentityHashMap<Block, Holder.Reference<Block>>());
            intrusiveHolderCacheField.set(Registry.ITEM, new IdentityHashMap<Item, Holder.Reference<Item>>());
            frozenField.set(Registry.BLOCK, false);
            frozenField.set(Registry.ITEM, false);

            // create a new custom leaves customBlock instance
            final LeavesBlock customBlock = new LeavesBlock(customLeafInfo.properties());
            leafInfoToLeavesBlockMap.put(customLeafInfo, customBlock);
            previousToNew.put(customLeafInfo.block(), customBlock);

            final Field mapField = CraftBlockData.class.getDeclaredField("MAP");
            mapField.setAccessible(true);
            final Map<Class<? extends Block>, com.google.common.base.Function<BlockState, CraftBlockData>> map = (Map<Class<? extends Block>, com.google.common.base.Function<BlockState, CraftBlockData>>) mapField.get(null);
            if (customLeafInfo.isMangrove()) {
                map.put(customBlock.getClass(), org.bukkit.craftbukkit.v1_19_R1.block.impl.CraftMangroveLeaves::new);
            } else {
                map.put(customBlock.getClass(), org.bukkit.craftbukkit.v1_19_R1.block.impl.CraftLeaves::new);
            }

            // get the blockById field in the customBlock registry
            byIdField.setAccessible(true);
            final ObjectList<Holder.Reference<Block>> blockById = (ObjectList<Holder.Reference<Block>>) byIdField.get(Registry.BLOCK);
            final ObjectList<Holder.Reference<Item>> itemById = (ObjectList<Holder.Reference<Item>>) byIdField.get(Registry.ITEM);
            blockById.size(blockById.size() + 1);
            for (CustomLeafInfo info : CUSTOM_LEAF_INFOS) {
                blockById.set(blockById.size() - 1, (Holder.Reference<Block>) info.block().defaultBlockState().getBlockHolder());
            }

            // get the toIdField in the customBlock registry and item registry
            final it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap<Block> blockToId = (it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap<Block>) toIdField.get(Registry.BLOCK);
            final it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap<Item> itemToId = (it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap<Item>) toIdField.get(Registry.ITEM);

            // set the custom leaf item's block to our custom block
            blockField.setAccessible(true);
            blockField.set(customLeafInfo.leafItem(), customBlock);

            final Map<String, ResourceKey<?>> blockValues = (Map<String, ResourceKey<?>>) valuesField.get(Registry.BLOCK);
            final Map<String, ResourceKey<?>> itemValues = (Map<String, ResourceKey<?>>) valuesField.get(Registry.ITEM);


            // get the resource key of the block
            final var resourceKey = (ResourceKey<Block>) blockValues.get(customLeafInfo.resourceKeyLocation());
            final var resourceKeyItem = (ResourceKey<Item>) itemValues.get("minecraft:item:minecraft:spruce_leaves");


            // find the ID of the leaf block
            int leafId = 0;
            for (var holder : blockById) {
                if (holder.key().equals(resourceKey)) {
                    break;
                }
                leafId++;
            }

            // get the blockByKey field
            final Map<ResourceKey<Block>, Holder.Reference<Block>> blockByKey = (Map<ResourceKey<Block>, Holder.Reference<Block>>) byKeyField.get(Registry.BLOCK);

            // get the current NMS leaf block
            final net.minecraft.world.level.block.LeavesBlock leavesBlock = (net.minecraft.world.level.block.LeavesBlock) Registry.BLOCK.get(resourceKey);

            // get the tags of the block
            // set the tags of the block to the tags of the customBlock
            blockTagsField.set(customBlock.defaultBlockState().getBlockHolder(), leavesBlock.defaultBlockState().getBlockHolder().tags().collect(Collectors.toSet()));

            // find the original block state ids of the original leaf block
            final List<Integer> ids = new ArrayList<>();
            for (BlockState state : leavesBlock.getStateDefinition().getPossibleStates()) {
                ids.add(Block.BLOCK_STATE_REGISTRY.getId(state));
            }

            // remove the old block so that we can insert our new one
            blockByKey.remove(resourceKey);
            // register our custom block with the correct id
            Registry.BLOCK.registerMapping(
                    leafId,
                    resourceKey,
                    customBlock,
                    Lifecycle.stable()
            );

            // get the blockState to id field from the block state registry
            final Object2IntMap<BlockState> tToId = (Object2IntMap<BlockState>) tToIdField.get(Block.BLOCK_STATE_REGISTRY);

            // get the id to blockState field from the block state registry
            final List<BlockState> idToT = (List<BlockState>) idToTField.get(Block.BLOCK_STATE_REGISTRY);

            // replace the old block states with our new ones
            int i = 0;
            for (BlockState state : customBlock.getStateDefinition().getPossibleStates()) {
                idToT.set(ids.get(i), state);
                tToId.put(state, (int) ids.get(i));
                i++;
            }


            // replace the old block -> item mapping to our new one
            Item.BY_BLOCK.put(customBlock, customLeafInfo.leafItem());

            // replace all the tree features that use the old NMS leaf states with our leaf state
            if (!customLeafInfo.isAzaleas()) {
                for (String field : customLeafInfo.treeFeatureFields()) {
                    final var treeFeatureField = TreeFeatures.class.getDeclaredField(field);
                    treeFeatureField.setAccessible(true);
                    final Holder<ConfiguredFeature<TreeConfiguration, ?>> treeFeature = (Holder<ConfiguredFeature<TreeConfiguration, ?>>) treeFeatureField.get(null);

                    // apply the new block state mapping
                    foliageProviderField.set(treeFeature.value().config(), SimpleStateProvider.simple(customBlock));
                }
            }


            HashMap<Block, Material> BLOCK_MATERIAL = (HashMap<Block, Material>) blockMaterialField.get(null);
            BLOCK_MATERIAL.put(customBlock, customLeafInfo.bukkitMaterial());

            // make our leaf flammable again
            final FireBlock blockfire = (FireBlock) Blocks.FIRE;
            setFlammableMethod.invoke(blockfire, customBlock, 30, 60);

            // rebuild the block cache and re-freeze the registries
            Blocks.rebuildCache();
            Registry.BLOCK.freeze();
            Registry.ITEM.freeze();

        } catch (NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new IllegalStateException("Failed to register custom leaf block");
        }
    }

    private void loadAzaleaTreeLeaves() {

        final LeavesBlock floweringAzaleaLeaves = this.leafInfoToLeavesBlockMap.get(FLOWERING_AZALEA_LEAVES);
        final LeavesBlock azaleaLeaves = this.leafInfoToLeavesBlockMap.get(AZALEA_LEAVES);
        try {
            for (String field : FLOWERING_AZALEA_LEAVES.treeFeatureFields()) {
                final var treeFeatureField = TreeFeatures.class.getDeclaredField(field);
                treeFeatureField.setAccessible(true);
                final Holder<ConfiguredFeature<TreeConfiguration, ?>> treeFeature = (Holder<ConfiguredFeature<TreeConfiguration, ?>>) treeFeatureField.get(null);

                foliageProviderField.set(
                        treeFeature.value().config(),
                        new WeightedStateProvider(
                                SimpleWeightedRandomList.<BlockState>builder()
                                        .add(azaleaLeaves.defaultBlockState(), 3)
                                        .add(floweringAzaleaLeaves.defaultBlockState(), 1)
                        )
                );
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            throw new IllegalStateException("Failed to register custom leaf block");
        }

    }

    public void handleChunkLoad(Chunk chunk, World world) {
        final PersistentDataContainer container = chunk.getPersistentDataContainer();
        if (this.pdcHelper.chunkHasLeafData(container)) {
            this.loadPDCData(chunk);
            return;
        }
        final UUID worldUUID = world.getUID();
        final ServerLevel level = ((CraftWorld) world).getHandle();
        final int chunkX = chunk.getX();
        final int chunkZ = chunk.getZ();

        Bukkit.getScheduler().runTaskLaterAsynchronously(this.getPlugin(), () -> {
            final Set<BlockPos> positions = new HashSet<>();
            final ChunkSnapshot snapshot = chunk.getChunkSnapshot();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                        final BlockData data = snapshot.getBlockData(x, y, z);
                        if (!(data instanceof Leaves)) continue;
                        positions.add(new BlockPos(chunkX * 16 + x, y, chunkZ * 16 + z));
                    }
                }
            }
            Bukkit.getScheduler().runTaskLater(this.getPlugin(), () -> {
                for (BlockPos pos : positions) {
                    final BlockState state = level.getBlockState(pos);
                    if (!(state.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock)) {
                        continue;
                    }

                    BlockState newState;
                    if (state.getBlock() instanceof final LeavesBlock leaves) {
                        newState = state.getBlock().defaultBlockState();
                    } else {
                        newState = this.previousToNew.get(state.getBlock()).defaultBlockState();
                    }
                    final FakeLeafData data = this.leafDataSupplier.getDefault();
                    final int actualDistance = state.getValue(LeavesBlock.DISTANCE);
                    data.actualDistance(actualDistance);
                    newState = newState.setValue(LeavesBlock.PERSISTENT, data.fakePersistence())
                            .setValue(LeavesBlock.DISTANCE, data.fakeDistance())
                            .setValue(LeavesBlock.WATERLOGGED, state.getValue(LeavesBlock.WATERLOGGED));
                    level.setBlock(pos, newState, 2 | 16 | 1024, 0);
                    this.leafDataSupplier.set(worldUUID, pos.getX(), pos.getY(), pos.getZ(), data);
                    final PersistentDataContainer blockData = new CustomBlockData(
                            chunk.getBlock(PositionUtil.getCoordInChunk(pos.getX()), pos.getY(), PositionUtil.getCoordInChunk(pos.getZ())),
                            this.getPlugin()
                    );
                    pdcHelper.setDistance(blockData, (byte) data.fakeDistance());
                    pdcHelper.setPersistent(blockData, data.fakePersistence() ? (byte) 1 : (byte) 0);
                    pdcHelper.setActualDistance(blockData, (byte) data.actualDistance());
                    pdcHelper.setActualPersistent(blockData, data.actualPersistence() ? (byte) 1 : (byte) 0);
                }
                this.pdcHelper.setChunkHasLeafData(chunk.getPersistentDataContainer(), true);
            }, 5);
        }, 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        final World world = event.getWorld();
        for (Chunk c : world.getLoadedChunks()) {
            this.handleChunkLoad(c, world);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        final Chunk chunk = event.getChunk();
        this.handleChunkLoad(chunk, event.getWorld());
    }

    private void loadPDCData(Chunk chunk) {
        final UUID world = chunk.getWorld().getUID();
        final var blocks = CustomBlockData.getBlocksWithCustomData(this.getPlugin(), chunk);
        for (var block : blocks) {
            final CustomBlockData blockData = new CustomBlockData(block, this.getPlugin());
            if (!Tag.LEAVES.isTagged(block.getType())) {
                this.pdcHelper.removeLeafBlockData(blockData);
                continue;
            }
            final Location location = block.getLocation();
            final FakeLeafData data = this.pdcHelper.getFakeLeafData(blockData);
            this.leafDataSupplier.set(world, location.getBlockX(), location.getBlockY(), location.getBlockZ(), data);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        final Chunk chunk = event.getChunk();
        this.pdcHelper.handleChunkUnload(chunk);
    }

    private JavaPlugin getPlugin() {
        if (this.plugin != null) return this.plugin;
        this.plugin = this.pluginSupplier.get();
        return this.plugin;
    }

}
