package io.github.fisher2911.hmcleaves.listener;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import io.github.fisher2911.hmcleaves.Config;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.LeafItem;
import io.github.fisher2911.hmcleaves.packet.PacketHelper;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import io.github.fisher2911.hmcleaves.util.PositionUtil;
import io.papermc.paper.event.block.BlockBreakBlockEvent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class PlaceListener implements Listener {

    private final HMCLeaves plugin;

    public PlaceListener(HMCLeaves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onClick(PlayerInteractEvent event) {
        this.handlePlace(event);
        this.handleInfoClick(event);
    }

    private void handlePlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        final Block toPlace = event.getClickedBlock().getRelative(event.getBlockFace());
        final ItemStack itemStack = event.getItem();
        if (itemStack == null) return;
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return;
        final String id = itemMeta.getPersistentDataContainer().get(PDCUtil.ITEM_KEY, PersistentDataType.STRING);
        final Chunk chunk = toPlace.getChunk();
        final Position2D chunkPos = new Position2D(toPlace.getWorld().getUID(), chunk.getX(), chunk.getZ());
        final Position position = new Position(PositionUtil.getCoordInChunk(toPlace.getX()), toPlace.getY(), PositionUtil.getCoordInChunk(toPlace.getZ()));
        if (id == null) {
            final Material material = itemStack.getType();
            if (!Tag.LEAVES.isTagged(material)) return;
            final WrappedBlockState defaultState = this.plugin.config().getDefaultState(material);
            Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, () -> {
                PacketHelper.sendLeaf(toPlace.getX(), toPlace.getY(), toPlace.getZ(), defaultState, Bukkit.getOnlinePlayers());
            }, 2);
            return;
        }
        final LeafItem leafItem = plugin.config().getItem(id);
        if (leafItem == null) return;
        toPlace.setType(leafItem.leafType());
        if (toPlace.getBlockData() instanceof final Leaves leaves) {
            leaves.setPersistent(true);
            toPlace.setBlockData(leaves);
        }
        final var defaultState = this.plugin.config().getDefaultState(leafItem.leafType()).clone();
        defaultState.setDistance(leafItem.distance());
        defaultState.setPersistent(leafItem.persistent());
        this.plugin.getLeafCache().addData(chunkPos, position, defaultState);
        final CustomBlockData customBlockData = new CustomBlockData(toPlace, this.plugin);
        customBlockData.set(PDCUtil.PERSISTENCE_KEY, PersistentDataType.BYTE, leafItem.persistent() ? (byte) 1 : (byte) 0);
        customBlockData.set(PDCUtil.DISTANCE_KEY, PersistentDataType.BYTE, (byte) leafItem.distance());
        PacketHelper.sendLeaf(toPlace.getX(), toPlace.getY(), toPlace.getZ(), defaultState, Bukkit.getOnlinePlayers());

        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) itemStack.setAmount(itemStack.getAmount() - 1);

        PacketHelper.sendArmSwing(event.getPlayer(), Bukkit.getOnlinePlayers());
    }

    private void handleInfoClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        final Block clicked = event.getClickedBlock();
        if (clicked == null || !Tag.LEAVES.isTagged(clicked.getType())) return;
        final ItemStack itemStack = event.getItem();
        if (itemStack == null) return;
        if (!Config.DEBUG_TOOL_ID.equals(PDCUtil.getLeafDataItemId(itemStack))) return;

        final Player player = event.getPlayer();
        if (!(clicked.getBlockData() instanceof final Leaves leaves)) return;
        player.sendMessage("Actual distance: " + leaves.getDistance() + " actual persistence: " + leaves.isPersistent());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onActionClick(InventoryCreativeEvent event) {
        if (event.getClick() != ClickType.CREATIVE) return;
        if (!(event.getWhoClicked() instanceof final Player player)) return;
        final RayTraceResult rayTrace = player.rayTraceBlocks(6);
        if (rayTrace == null) return;
        final Block clicked = rayTrace.getHitBlock();
        if (clicked == null) return;
        final Location location = clicked.getLocation();
        final Position2D chunkPos = new Position2D(location.getWorld().getUID(), location.getChunk().getX(), location.getChunk().getZ());
        final Position position = new Position(PositionUtil.getCoordInChunk(location.getBlockX()), location.getBlockY(), PositionUtil.getCoordInChunk(location.getBlockZ()));
        final var state = this.plugin.getLeafCache().getAt(chunkPos, position);
        if (state == null) return;
        final LeafItem leafItem = this.plugin.config().getByState(state);
        if (leafItem == null) return;
        final ItemStack leafItemStack = leafItem.itemStack();
        for (int i = 0; i <= 8; i++) {
            final ItemStack itemStack = player.getInventory().getItem(i);
            if (leafItemStack.equals(itemStack)) {
                player.getInventory().setHeldItemSlot(i);
                event.setCancelled(true);
                return;
            }
        }
        event.setCursor(leafItemStack);
    }

    @Nullable
    private WrappedBlockState removeBlock(Block block, Collection<? extends Player> players) {
        final Chunk chunk = block.getChunk();
        final Position2D chunkPos = new Position2D(block.getWorld().getUID(), chunk.getX(), chunk.getZ());
        final Position position = new Position(PositionUtil.getCoordInChunk(block.getX()), block.getY(), PositionUtil.getCoordInChunk(block.getZ()));
        final var previous = this.plugin.getLeafCache().remove(chunkPos, position);
        if (!(block.getBlockData() instanceof Leaves)) return null;
        final CustomBlockData blockData = new CustomBlockData(block, this.plugin);
        blockData.remove(PDCUtil.PERSISTENCE_KEY);
        blockData.remove(PDCUtil.DISTANCE_KEY);
        Bukkit.getScheduler().runTaskLaterAsynchronously(
                this.plugin,
                () -> {
                    PacketHelper.sendBlock(
                            block.getX(),
                            block.getY(),
                            block.getZ(),
                            WrappedBlockState.getDefaultState(
                                    PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                                    StateTypes.AIR
                            ),
                            players.toArray(new Player[0])
                    );
                },
                1
        );
        return previous;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onTreeGrow(StructureGrowEvent event) {
        for (BlockState block : event.getBlocks()) {
            final Material material = block.getType();
            if (!Tag.LEAVES.isTagged(material)) continue;
            final var state = this.plugin.config().getDefaultState(material).clone();
            PacketHelper.sendLeaf(block.getX(), block.getY(), block.getZ(), state, Bukkit.getOnlinePlayers());
            final CustomBlockData customBlockData = new CustomBlockData(block.getBlock(), this.plugin);
            customBlockData.set(PDCUtil.PERSISTENCE_KEY, PersistentDataType.BYTE, state.isPersistent() ? (byte) 1 : (byte) 0);
            customBlockData.set(PDCUtil.DISTANCE_KEY, PersistentDataType.BYTE, (byte) state.getDistance());
            this.plugin.getLeafCache().addData(new Position2D(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()), new Position(PositionUtil.getCoordInChunk(block.getX()), block.getY(), PositionUtil.getCoordInChunk(block.getZ())), state);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onCustomBlockBreak(CustomBlockDataRemoveEvent event) {
        final Block block = event.getBlock();
        if (!(block.getBlockData() instanceof Leaves)) return;
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.removeBlock(block, Bukkit.getOnlinePlayers()), 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onRemove(BlockBreakEvent event) {
        if (event instanceof LeafBlockBreakEvent) return;
        final Block block = event.getBlock();
        if (!(block.getBlockData() instanceof Leaves)) return;
        final var state = this.removeBlock(block, List.of(event.getPlayer()));
        if (state == null) return;
        event.setCancelled(true);
        if (!(block.getBlockData() instanceof Leaves leaves)) return;
        leaves.setPersistent(state.isPersistent());
        leaves.setDistance(state.getDistance());
        block.setBlockData(leaves);
        final var breakEvent = new LeafBlockBreakEvent(block, event.getPlayer());
        Bukkit.getPluginManager().callEvent(breakEvent);
        if (breakEvent.isCancelled()) return;
        block.setType(Material.AIR);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onRemove(BlockDestroyEvent event) {
        if (event instanceof LeafBlockDestroyEvent) return;
        final Block block = event.getBlock();
        if (!(block.getBlockData() instanceof Leaves)) return;
        final var state = this.removeBlock(block, Bukkit.getOnlinePlayers());
        if (state == null) return;
        event.setCancelled(true);
        if (!(block.getBlockData() instanceof Leaves leaves)) return;
        leaves.setPersistent(state.isPersistent());
        leaves.setDistance(state.getDistance());
        block.setBlockData(leaves);
        final var breakEvent = new LeafBlockDestroyEvent(block, event.getNewState(), event.willDrop());
        Bukkit.getPluginManager().callEvent(breakEvent);
        if (breakEvent.isCancelled()) return;
        block.setType(Material.AIR);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onRemove(BlockExplodeEvent event) {
        if (event instanceof LeafBlockExplodeEvent) return;
        boolean changed = false;
        for (Block block : event.blockList()) {
            if (!(block.getBlockData() instanceof Leaves)) continue;
            final var state = this.removeBlock(block, event.getBlock().getWorld().getPlayers());
            if (state == null) continue;
            if (!(block.getBlockData() instanceof Leaves leaves)) continue;
            leaves.setPersistent(state.isPersistent());
            leaves.setDistance(state.getDistance());
            block.setBlockData(leaves);
            changed = true;
        }
        if (!changed) return;
        event.setCancelled(true);
        final var explodeEvent = new LeafBlockExplodeEvent(event.getBlock(), event.blockList(), event.getYield());
        Bukkit.getPluginManager().callEvent(explodeEvent);
        if (explodeEvent.isCancelled()) return;
        for (Block b : event.blockList()) {
            b.setType(Material.AIR);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onRemove(EntityExplodeEvent event) {
        if (event instanceof LeafEntityExplodeEvent) return;
        boolean changed = false;
        for (Block block : event.blockList()) {
            if (!(block.getBlockData() instanceof Leaves)) continue;
            final var state = this.removeBlock(block, event.getEntity().getWorld().getPlayers());
            if (state == null) continue;
            if (!(block.getBlockData() instanceof Leaves leaves)) continue;
            leaves.setPersistent(state.isPersistent());
            leaves.setDistance(state.getDistance());
            block.setBlockData(leaves);
            changed = true;
        }
        if (!changed) return;
        event.setCancelled(true);
        final var explodeEvent = new LeafEntityExplodeEvent(event.getEntity(), event.getLocation(), event.blockList(), event.getYield());
        Bukkit.getPluginManager().callEvent(explodeEvent);
        if (explodeEvent.isCancelled()) return;
        for (Block b : event.blockList()) {
            b.setType(Material.AIR);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onRemove(LeavesDecayEvent event) {
        if (event instanceof LeafLeavesDecayEvent) return;
        final Block block = event.getBlock();
        if (!(block.getBlockData() instanceof Leaves)) return;
        final var state = this.removeBlock(block, Bukkit.getOnlinePlayers());
        if (state == null) return;
        event.setCancelled(true);
        if (!(block.getBlockData() instanceof Leaves leaves)) return;
        leaves.setPersistent(state.isPersistent());
        leaves.setDistance(state.getDistance());
        block.setBlockData(leaves);
        final var breakEvent = new LeafLeavesDecayEvent(block);
        Bukkit.getPluginManager().callEvent(breakEvent);
        if (breakEvent.isCancelled()) return;
        block.setType(Material.AIR);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onRemove(BlockBreakBlockEvent event) {
        if (event instanceof LeafBlockBreakBlockEvent) return;
        final Block block = event.getBlock();
        if (!(block.getBlockData() instanceof Leaves)) return;
        final var state = this.removeBlock(block, Bukkit.getOnlinePlayers());
        if (state == null) return;
        if (!(block.getBlockData() instanceof Leaves leaves)) return;
        leaves.setPersistent(state.isPersistent());
        leaves.setDistance(state.getDistance());
        block.setBlockData(leaves);
        final var breakEvent = new LeafLeavesDecayEvent(block);
        Bukkit.getPluginManager().callEvent(breakEvent);
        if (breakEvent.isCancelled()) return;
        block.setType(Material.AIR);
    }

    private static class LeafBlockBreakEvent extends BlockBreakEvent {

        public LeafBlockBreakEvent(@NotNull Block theBlock, @NotNull Player player) {
            super(theBlock, player);
        }

    }

    private static class LeafBlockDestroyEvent extends BlockDestroyEvent {

        public LeafBlockDestroyEvent(@NotNull Block block, @NotNull BlockData newState, boolean willDrop) {
            super(block, newState, willDrop);
        }

    }

    private static class LeafBlockBreakBlockEvent extends BlockBreakBlockEvent {

        public LeafBlockBreakBlockEvent(@NotNull Block block, @NotNull Block source, @NotNull List<ItemStack> drops) {
            super(block, source, drops);
        }
    }

    private static class LeafEntityExplodeEvent extends EntityExplodeEvent {

        public LeafEntityExplodeEvent(@NotNull Entity what, @NotNull Location location, @NotNull List<Block> blocks, float yield) {
            super(what, location, blocks, yield);
        }

    }

    private static class LeafBlockExplodeEvent extends BlockExplodeEvent {

        public LeafBlockExplodeEvent(@NotNull Block what, @NotNull List<Block> blocks, float yield) {
            super(what, blocks, yield);
        }

    }

    private static class LeafLeavesDecayEvent extends LeavesDecayEvent {

        public LeafLeavesDecayEvent(@NotNull Block what) {
            super(what);
        }

    }

}
