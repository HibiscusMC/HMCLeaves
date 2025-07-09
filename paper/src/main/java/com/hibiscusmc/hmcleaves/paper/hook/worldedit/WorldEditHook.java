package com.hibiscusmc.hmcleaves.paper.hook.worldedit;

import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlock;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlockState;
import com.hibiscusmc.hmcleaves.paper.config.LeavesConfig;
import com.hibiscusmc.hmcleaves.paper.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.paper.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorld;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorldManager;
import com.hibiscusmc.hmcleaves.paper.world.Position;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Axis;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.enginehub.linbus.tree.LinByteTag;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinIntTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTagType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class WorldEditHook {

    private static final String DISTANCE_PROPERTY = "distance";
    private static final String WATERLOGGED_PROPERTY = "waterlogged";
    private static final String PERSISTENT_PROPERTY = "persistent";
    private static final String AXIS_PROPERTY = "axis";

    private static final String BLOCK_ID = "hmcleaves:block_id";
    public static final String PROPERTIES_TAG = "hmcleaves:block_properties";

    private final LeavesConfig leavesConfig;
    private final LeavesWorldManager worldManager;

    public WorldEditHook(HMCLeaves plugin) {
        this.leavesConfig = plugin.leavesConfig();
        this.worldManager = plugin.worldManager();
    }

    public void load() {
        WorldEdit.getInstance().getEventBus().register(this);
        HMCLeaves.getPlugin(HMCLeaves.class).log(Level.INFO, "Registered WorldEdit Hook");
    }

    private static final String BUKKIT_NBT_TAG = "PublicBukkitValues";

    public void trySaveSchematic(Player player) {
        try {
            final ClipboardHolder clipboardHolder = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player)).getClipboard();
            final Clipboard clipboard = clipboardHolder.getClipboard();
            final Region region = clipboard.getRegion();
            final BlockVector3 min = region.getMinimumPoint();
            final BlockVector3 max = region.getMaximumPoint();
            final com.sk89q.worldedit.world.World worldEditWorld = region.getWorld();
            if (worldEditWorld == null) {
                player.sendMessage(Component.text("This region does not have a valid world!").color(NamedTextColor.RED));
                return;
            }
            final World world = BukkitAdapter.adapt(region.getWorld());
            final UUID worldId = world.getUID();
            final LeavesWorld leavesWorld = this.worldManager.getWorld(worldId);
            if (leavesWorld == null) {
                player.sendMessage(Component.text("This world is not loaded for HMCLeaves!").color(NamedTextColor.RED));
                return;
            }
            int transformedBlocks = 0;
            final Set<ChunkPosition> missingChunks = new HashSet<>();
            for (int x = min.x(); x <= max.x(); x++) {
                for (int y = min.y(); y <= max.y(); y++) {
                    for (int z = min.z(); z <= max.z(); z++) {
                        final Position position = Position.at(worldId, x, y, z);
                        final ChunkPosition chunkPosition = position.toChunkPosition();
                        if (missingChunks.contains(chunkPosition)) {
                            continue;
                        }
                        final LeavesChunk leavesChunk = leavesWorld.getChunk(chunkPosition);
                        if (leavesChunk == null) {
                            player.sendMessage(Component.text("Chunk " + chunkPosition.x() + " " + chunkPosition.z() + " is not loaded for HMCLeaves!").color(NamedTextColor.RED));
                            missingChunks.add(chunkPosition);
                            continue;
                        }
                        final CustomBlockState customBlockState = leavesChunk.getBlock(position);
                        if (customBlockState == null) {
                            continue;
                        }
                        final BaseBlock furnaceBlock = BlockTypes.FURNACE.getDefaultState().toBaseBlock();
                        LinCompoundTag baseNBT = furnaceBlock.getNbt();
                        if (baseNBT == null) {
                            baseNBT = LinCompoundTag.builder().build();
                        }
                        LinCompoundTag bukkitTag;
                        if (!(baseNBT.findTag(BUKKIT_NBT_TAG, LinTagType.compoundTag()) instanceof final LinCompoundTag tag)) {
                            bukkitTag = LinCompoundTag.builder().build();
                        } else {
                            bukkitTag = tag;
                        }

                        final LinCompoundTag.Builder propertiesBuilder = LinCompoundTag.builder();
                        final BlockState state = clipboard.getBlock(BlockVector3.at(x, y, z));
                        final BlockData blockData = BukkitAdapter.adapt(state);
                        switch (blockData) {
                            case Leaves leaves -> {
                                final int distance = leaves.getDistance();
                                final boolean persistent = leaves.isPersistent();
                                final boolean waterlogged = leaves.isWaterlogged();
                                propertiesBuilder.put(DISTANCE_PROPERTY, LinIntTag.of(distance));
                                propertiesBuilder.put(PERSISTENT_PROPERTY, LinByteTag.of(persistent ? (byte) 1 : (byte) 0));
                                propertiesBuilder.put(WATERLOGGED_PROPERTY, LinByteTag.of(waterlogged ? (byte) 1 : (byte) 0));
                            }
                            case Orientable orientable -> {
                                final Axis axis = orientable.getAxis();
                                propertiesBuilder.put(AXIS_PROPERTY, LinStringTag.of(axis.name()));
                            }
                            default -> {
                            }
                        }
                        final LinCompoundTag.Builder newBukkitValuesTagBuilder = bukkitTag.toBuilder();
                        newBukkitValuesTagBuilder.put(BLOCK_ID, LinStringTag.of(customBlockState.customBlock().id()));
                        newBukkitValuesTagBuilder.put(PROPERTIES_TAG, propertiesBuilder.build());
                        clipboard.setBlock(BlockVector3.at(x, y, z), furnaceBlock.toBaseBlock(
                                baseNBT.toBuilder()
                                        .put(BUKKIT_NBT_TAG, newBukkitValuesTagBuilder.build())
                                        .build()
                        ));
                        transformedBlocks++;
                    }
                }
            }
            player.sendMessage(Component.text("Successfully saved schematic! " + transformedBlocks + " leaves were transformed.").color(NamedTextColor.GREEN));
        } catch (EmptyClipboardException e) {
            player.sendMessage(Component.text("You do not have a clipboard selected!").color(NamedTextColor.RED));
            throw new RuntimeException(e);
        } catch (WorldEditException e) {
            player.sendMessage(Component.text("An error occurred while transforming the schematic!").color(NamedTextColor.RED));
            throw new RuntimeException(e);
        }
    }

    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
            @Override
            public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 pos, T block) throws WorldEditException {
                final com.sk89q.worldedit.world.World worldEditWorld = event.getWorld();
                if (worldEditWorld == null) {
                    throw new IllegalStateException("WorldEdit world is null in EditSessionEvent");
                }
                final Position position = new Position(BukkitAdapter.adapt(worldEditWorld).getUID(), pos.x(), pos.y(), pos.z());
                if (!(block instanceof final BaseBlock baseBlock)) return super.setBlock(pos, block);
                if (baseBlock.getBlockType() != BlockTypes.FURNACE) return super.setBlock(pos, block);
                final LinCompoundTag baseNBT = baseBlock.getNbt();
                if (baseNBT == null) {
                    return super.setBlock(pos, block);
                }
                if (!(baseNBT.getTag(BUKKIT_NBT_TAG, LinTagType.compoundTag()) instanceof final LinCompoundTag bukkitTag)) {
                    return super.setBlock(pos, block);
                }
                final LinStringTag blockIdTag = bukkitTag.getTag(BLOCK_ID, LinTagType.stringTag());
                if (blockIdTag == null) {
                    return super.setBlock(pos, block);
                }
                final String blockId = blockIdTag.value();
                if (blockId == null) {
                    return super.setBlock(pos, block);
                }
                final CustomBlock customBlock = WorldEditHook.this.leavesConfig.getBlock(blockId);
                if (customBlock == null) {
                    return super.setBlock(pos, block);
                }
                final BlockData bukkitBlockData = BukkitAdapter.adapt(baseBlock);
                final CustomBlockState customBlockState = customBlock.getBlockStateFromWorldBlock(bukkitBlockData);
                if (customBlockState == null) {
                    return super.setBlock(pos, block);
                }
                final LeavesWorld leavesWorld = WorldEditHook.this.worldManager.getWorld(position.worldId());
                if (leavesWorld == null) {
                    return super.setBlock(pos, block);
                }
                final ChunkPosition chunkPosition = position.toChunkPosition();
                leavesWorld.editInsertChunk(chunkPosition, leavesChunk -> leavesChunk.setBlock(position, customBlockState));
                final BlockData blockData = customBlock.worldMaterial().createBlockData();
                final LinCompoundTag propertiesTag = bukkitTag.getTag(PROPERTIES_TAG, LinTagType.compoundTag());
                if (propertiesTag != null) {
                    switch (blockData) {
                        case Leaves leaves -> {
                            final LinIntTag distanceStringTag = propertiesTag.findTag(DISTANCE_PROPERTY, LinTagType.intTag());
                            if (distanceStringTag != null) {
                                leaves.setDistance(distanceStringTag.value());
                            }
                            final LinByteTag waterloggedStringTag = propertiesTag.findTag(WATERLOGGED_PROPERTY, LinTagType.byteTag());
                            if (waterloggedStringTag != null) {
                                leaves.setWaterlogged(waterloggedStringTag.value() == 1);
                            }
                            final LinByteTag persistentTag = propertiesTag.findTag(PERSISTENT_PROPERTY, LinTagType.byteTag());
                            if (persistentTag != null) {
                                leaves.setPersistent(persistentTag.value() == 1);
                            }
                        }
                        case Orientable orientable -> {
                            final LinStringTag axisTag = propertiesTag.findTag(AXIS_PROPERTY, LinTagType.stringTag());
                            if (axisTag != null) {
                                orientable.setAxis(Axis.valueOf(axisTag.value()));
                            }
                        }
                        default -> {
                        }
                    }
                }
                return super.setBlock(pos, BukkitAdapter.adapt(blockData));
            }
        });
    }
}
