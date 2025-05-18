package com.hibiscusmc.hmcleaves.paper.listener;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.hibiscusmc.hmcleaves.common.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.common.world.ChunkSectionPosition;
import com.hibiscusmc.hmcleaves.common.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.common.world.LeavesWorld;
import com.hibiscusmc.hmcleaves.common.world.Position;
import com.hibiscusmc.hmcleaves.paper.HMCLeavesPlugin;
import com.hibiscusmc.hmcleaves.common.block.LeavesBlock;
import com.hibiscusmc.hmcleaves.paper.config.BlockDropConfig;
import com.hibiscusmc.hmcleaves.paper.config.LeavesConfigImplementation;
import com.hibiscusmc.hmcleaves.common.world.LeavesWorldManager;
import com.hibiscusmc.hmcleaves.paper.packet.PacketUtil;
import com.hibiscusmc.hmcleaves.paper.util.PlayerUtils;
import com.hibiscusmc.hmcleaves.paper.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class LeavesListener implements Listener {

    private final HMCLeavesPlugin plugin;
    private final LeavesWorldManager worldManager;
    private final LeavesConfigImplementation config;

    public LeavesListener(HMCLeavesPlugin plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.worldManager();
        this.config = plugin.leavesConfig();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlace(PlayerInteractEvent event) {
        final Action action = event.getAction();
        final Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final ItemStack itemStack = event.getItem();
        if (itemStack == null) {
            return;
        }
        final World world = player.getWorld();
        if (!this.config.isWorldWhitelisted(world.getName())) {
            return;
        }
        final LeavesWorld leavesWorld = this.worldManager.getWorld(world.getUID());
        if (leavesWorld == null) {
            event.setCancelled(true);
            return;
        }

        final Block block = event.getClickedBlock();
        if (block == null) {
            event.setCancelled(true);
            return;
        }
        final Block relativeBlock = block.getRelative(event.getBlockFace());

        final Position position;
        if (block.isReplaceable() || block.getType().isAir() || Tag.REPLACEABLE.isTagged(block.getType())) {
            position = WorldUtil.convertLocation(block.getLocation());
        } else {
            position = WorldUtil.convertLocation(relativeBlock.getLocation());
        }
        final ChunkPosition chunkPosition = position.toChunkPosition();
        final LeavesChunk chunk = leavesWorld.getChunk(chunkPosition);
        if (chunk != null && this.config.isDebugItem(itemStack)) {
            final LeavesBlock leavesBlock = chunk.getBlock(WorldUtil.convertLocation(block.getLocation()));
            if (leavesBlock != null && player.hasPermission(LeavesConfigImplementation.PLACE_DECAYABLE_PERMISSION)) {
                this.config.sendDebugInfo(player, leavesBlock, block.getBlockData());
            }
            event.setCancelled(true);
            return;
        }

        final LeavesBlock leavesBlock = this.config.getBlockFromItem(itemStack);
        if (leavesBlock == null) {
            return;
        }
        if (!relativeBlock.getType().isAir() && !relativeBlock.isReplaceable() && !Tag.REPLACEABLE.isTagged(relativeBlock.getType())) {
            event.setCancelled(true);
            return;
        }
        if (!player.isSneaking() && block.getBlockData().getMaterial().isInteractable()) {
            return;
        }
        final BlockData data = config.getWorldBlockData(leavesBlock.id());
        if (data == null) {
            event.setCancelled(true);
            this.plugin.getLogger().severe("Failed to get block data for " + leavesBlock.id());
            return;
        }
        final Location placeLocation = WorldUtil.convertPosition(world, position);
        if (data instanceof final Leaves leaves) {
            leaves.setPersistent(!this.config.isPlacingDecayable(player));
            if (placeLocation.getBlock().getType() == Material.WATER) {
                leaves.setWaterlogged(true);
            }
        }
        final Location center = placeLocation.clone().add(0.5, 0.5, 0.5);
        if (!world.getNearbyEntities(center, 0.5, 0.5, 0.5, LivingEntity.class::isInstance).isEmpty()) {
            event.setCancelled(true);
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
            final int amount = itemStack.getAmount();
            if (amount <= 1) {
                player.getInventory().setItemInMainHand(null);
            } else {
                itemStack.setAmount(amount - 1);
            }
        }
        leavesWorld.editInsertChunk(chunkPosition, leavesChunk -> leavesChunk.setBlock(position, leavesBlock));
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> relativeBlock.setBlockData(data), 1);
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            PacketUtil.sendArmSwing(player);
            PacketUtil.sendSingleBlockChange(leavesBlock.blockState().get(), position, PlayerUtils.getNearbyPlays(world.getUID(), chunkPosition));
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        final Block block = event.getBlock();
        final World world = block.getWorld();
        if (!this.config.isWorldWhitelisted(world.getName())) {
            return;
        }
        final ItemStack itemStack = event.getItemInHand();
        if (itemStack == null) {
            return;
        }
        final LeavesBlock leavesBlock = this.config.getBlockFromItem(itemStack);
        if (leavesBlock == null) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        this.removeBlock(event.getBlock(), leavesBlock -> {
            if (!event.isDropItems()) {
                return;
            }
            event.setDropItems(false);
            final Block block = event.getBlock();
            final Location location = block.getLocation();
            final BlockDropConfig drops = this.config.getBlockDrops(leavesBlock.id());
            if (drops == null) {
                return;
            }
            final Player player = event.getPlayer();
            final ItemStack itemStack = player.getInventory().getItemInMainHand();
            if ((itemStack == null || itemStack.getType() != Material.SHEARS) && drops.requiresShears()) {
                return;
            }
            final ItemStack dropItem = drops.copyLeavesItem();
            if (dropItem == null) {
                return;
            }
            final World world = block.getWorld();
            world.dropItem(location, dropItem);
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        this.onPistonEvent(event.getBlock().getWorld(), event.getBlocks(), event.getDirection());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        this.onPistonEvent(event.getBlock().getWorld(), event.getBlocks(), event.getDirection());
    }

    private void onPistonEvent(World world, Collection<Block> blocks, BlockFace direction) {
        if (!this.config.isWorldWhitelisted(world.getName())) {
            return;
        }
        final LeavesWorld leavesWorld = this.worldManager.getWorld(world.getUID());
        if (leavesWorld == null) {
            return;
        }
        final Map<Position, LeavesBlock> insert = new HashMap<>();
        for (Block block : blocks) {
            final Position position = WorldUtil.convertLocation(block.getLocation());
            final ChunkPosition chunkPosition = position.toChunkPosition();
            final LeavesChunk chunk = leavesWorld.getChunk(chunkPosition);
            if (chunk == null) {
                continue;
            }
            final LeavesBlock leavesBlock = chunk.getBlock(position);
            if (leavesBlock == null) {
                continue;
            }
            final PistonMoveReaction reaction = block.getPistonMoveReaction();
            if (reaction == PistonMoveReaction.BLOCK) {
                continue;
            }
            if (reaction == PistonMoveReaction.BREAK) {
                leavesWorld.editInsertChunk(chunkPosition, leavesChunk -> leavesChunk.removeBlock(position));
                continue;
            }
            if (reaction == PistonMoveReaction.MOVE) {
                final Position newPosition = position.add(direction.getModX(), direction.getModY(), direction.getModZ());
                leavesWorld.editInsertChunk(chunkPosition, leavesChunk -> {
                    leavesChunk.removeBlock(position);
                    insert.put(newPosition, leavesBlock);
                });
            }
        }
        for (Map.Entry<Position, LeavesBlock> entry : insert.entrySet()) {
            final Position position = entry.getKey();
            final ChunkPosition chunkPosition = position.toChunkPosition();
            leavesWorld.editInsertChunk(chunkPosition, leavesChunk -> {
                leavesChunk.setBlock(position, entry.getValue());
            });
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTreeGrow(StructureGrowEvent event) {
        final LeavesWorld leavesWorld = this.worldManager.getWorld(event.getWorld().getUID());
        if (leavesWorld == null) {
            return;
        }
        event.getBlocks().forEach(block -> {
            final LeavesBlock defaultBlock = this.config.getLeavesBlockFromWorldBlockData(block.getBlockData());
            if (defaultBlock == null) {
                return;
            }
            final Position position = WorldUtil.convertLocation(block.getLocation());
            final ChunkPosition chunkPosition = position.toChunkPosition();
            final LeavesChunk chunk = leavesWorld.getChunk(chunkPosition);
            if (chunk == null) {
                return;
            }
            chunk.setBlock(position, defaultBlock);
        });
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        final Block block = event.getBlock();
        final World world = block.getWorld();
        if (!this.config.isWorldWhitelisted(world.getName())) {
            return;
        }
        final LeavesWorld leavesWorld = this.worldManager.getWorld(world.getUID());
        if (leavesWorld == null) {
            return;
        }
        final Position position = WorldUtil.convertLocation(block.getLocation());
        final ChunkPosition chunkPosition = position.toChunkPosition();
        final LeavesChunk chunk = leavesWorld.getChunk(chunkPosition);
        if (chunk == null) {
            return;
        }
        final LeavesBlock leavesBlock = chunk.getBlock(position);
        if (leavesBlock == null) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            chunk.removeBlock(position);
            PacketUtil.sendSingleBlockChange(WrappedBlockState.getDefaultState(StateTypes.AIR), position, PlayerUtils.getNearbyPlays(world.getUID(), chunkPosition));
        }, 1);
    }

    @EventHandler
    public void onBlockDropItemsEvent(ItemSpawnEvent event) {
        final Location location = event.getLocation();
        final ItemStack itemStack = event.getEntity().getItemStack();
        final World world = location.getWorld();
        final LeavesWorld leavesWorld = this.worldManager.getWorld(world.getUID());
        if (leavesWorld == null) {
            return;
        }
        final Position position = WorldUtil.convertLocation(location);
        final ChunkPosition chunkPosition = position.toChunkPosition();
        final LeavesChunk chunk = leavesWorld.getChunk(chunkPosition);
        if (chunk == null) {
            return;
        }
        final LeavesBlock leavesBlock = chunk.getBlock(position);
        if (leavesBlock == null) {
            return;
        }
        final BlockDropConfig drops = this.config.getBlockDrops(leavesBlock.id());
        if (drops == null) {
            return;
        }
        if (!Tag.SAPLINGS.isTagged(itemStack.getType())) {
            return;
        }
        final ItemStack sapling = drops.copySapling();
        if (sapling == null) {
            return;
        }
        event.getEntity().setItemStack(sapling);
        chunk.removeBlock(position);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockDestroy(BlockDestroyEvent event) {
        final Block block = event.getBlock();
        final World world = block.getWorld();
        if (!this.config.isWorldWhitelisted(world.getName())) {
            return;
        }
        final LeavesWorld leavesWorld = this.worldManager.getWorld(world.getUID());
        if (leavesWorld == null) {
            return;
        }
        final Position position = WorldUtil.convertLocation(block.getLocation());
        final ChunkPosition chunkPosition = position.toChunkPosition();
        final LeavesChunk chunk = leavesWorld.getChunk(chunkPosition);
        if (chunk == null) {
            return;
        }
        final LeavesBlock leavesBlock = chunk.getBlock(position);
        if (leavesBlock == null) {
            return;
        }
        leavesWorld.editInsertChunk(chunkPosition, leavesChunk -> leavesChunk.removeBlock(position));
    }

    private boolean removeBlock(Block block, Consumer<LeavesBlock> dropHandler) {
        final World world = block.getWorld();
        if (!this.config.isWorldWhitelisted(world.getName())) {
            return true;
        }
        final LeavesWorld leavesWorld = this.worldManager.getWorld(block.getWorld().getUID());
        if (leavesWorld == null) {
            return false;
        }
        final Position position = WorldUtil.convertLocation(block.getLocation());
        final ChunkPosition chunkPosition = position.toChunkPosition();
        final LeavesChunk chunk = leavesWorld.getChunk(chunkPosition);
        if (chunk == null) {
            return false;
        }
        final LeavesBlock leavesBlock = chunk.getBlock(position);
        if (leavesBlock == null) {
            return false;
        }
        leavesWorld.editInsertChunk(chunkPosition, leavesChunk -> leavesChunk.removeBlock(position));
        dropHandler.accept(leavesBlock);
        return true;
    }


}
