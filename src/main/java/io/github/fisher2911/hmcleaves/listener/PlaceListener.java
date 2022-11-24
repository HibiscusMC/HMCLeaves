package io.github.fisher2911.hmcleaves.listener;

import io.github.fisher2911.hmcleaves.Config;
import io.github.fisher2911.hmcleaves.FakeLeafState;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.LeafCache;
import io.github.fisher2911.hmcleaves.LeafData;
import io.github.fisher2911.hmcleaves.LeafItem;
import io.github.fisher2911.hmcleaves.api.HMCLeavesAPI;
import io.github.fisher2911.hmcleaves.packet.PacketHelper;
import io.github.fisher2911.hmcleaves.util.ChunkUtil;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;

public class PlaceListener implements Listener {

    private final HMCLeaves plugin;
    private final LeafCache leafCache;
    private final Config config;

    public PlaceListener(HMCLeaves plugin) {
        this.plugin = plugin;
        this.leafCache = plugin.getLeafCache();
        this.config = plugin.config();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onClick(PlayerInteractEvent event) {
        this.handlePlace(event);
        this.handleInfoClick(event);
    }

    private void handlePlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        final Block toPlace = event.getClickedBlock().getRelative(event.getBlockFace());
        final World world = toPlace.getWorld();
        if (!(world.getNearbyEntities(toPlace.getLocation().clone().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5, LivingEntity.class::isInstance).isEmpty()))
            return;
        final ItemStack itemStack = event.getItem();
        if (itemStack == null) return;
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return;
        final String id = PDCUtil.getLeafDataItemId(itemStack);/* itemMeta.getPersistentDataContainer().get(PDCUtil.ITEM_KEY, PersistentDataType.STRING);*/
        final Chunk chunk = toPlace.getChunk();
        final Position2D chunkPos = new Position2D(toPlace.getWorld().getUID(), chunk.getX(), chunk.getZ());
        final Position position = new Position(ChunkUtil.getCoordInChunk(toPlace.getX()), toPlace.getY(), ChunkUtil.getCoordInChunk(toPlace.getZ()));
        final LeafItem leafItem = plugin.config().getItem(id);
        LeafData leafData = leafItem != null ? leafItem.leafData() : PDCUtil.getLeafData(itemMeta.getPersistentDataContainer());
        if (leafData == null) {
            final Material material = itemStack.getType();
            if (!Tag.LEAVES.isTagged(material)) return;
//            final FakeLeafState defaultState = this.plugin.config().getDefaultState(material);
//            Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, () -> {
//                PacketHelper.sendBlock(toPlace.getWorld().getUID(), toPlace.getX(), toPlace.getY(), toPlace.getZ(), defaultState.state(), Bukkit.getOnlinePlayers());
//            }, 2);
//            return;
            leafData = new LeafData(material, this.plugin.config().getDefaultDistance(), this.plugin.config().isDefaultPersistent(), true);
        }
//        final FakeLeafState defaultFakeLeafState = this.plugin.config().getDefaultState(leafData.material());
//        final var defaultState = defaultFakeLeafState.state();
//        defaultState.setDistance(leafData.distance());
//        defaultState.setPersistent(leafData.persistent());
//        final FakeLeafState setState = new FakeLeafState(defaultState, leafData.actuallyPersistent(), 7);
//        this.plugin.getLeafCache().addData(chunkPos, position, setState);
//        toPlace.setType(leafData.material(), false);
//        if (toPlace.getBlockData() instanceof final Leaves leaves) {
//            leaves.setPersistent(true);
//            toPlace.setBlockData(leaves, false);
//        }
        HMCLeavesAPI.getInstance().setLeafAt(
                toPlace.getLocation(),
                leafData
        );

//        placeLeaf(toPlace);
//        LeafUpdater3.scheduleTick(toPlace.getLocation());
//        LeafUpdater.doUpdate(Map.of(toPlace.getLocation(), toPlace.getBlockData()));

//        final CustomBlockData customBlockData = new CustomBlockData(toPlace, this.plugin);
//
//        customBlockData.set(PDCUtil.PERSISTENCE_KEY, PersistentDataType.BYTE, leafData.persistent() ? (byte) 1 : (byte) 0);
//        customBlockData.set(PDCUtil.DISTANCE_KEY, PersistentDataType.BYTE, (byte) leafData.distance());
        Bukkit.getScheduler().runTaskAsynchronously(
                this.plugin,
                () -> PacketHelper.sendArmSwing(event.getPlayer(), Bukkit.getOnlinePlayers())
        );
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) itemStack.setAmount(itemStack.getAmount() - 1);
//        LeafUtil.checkPlaceLeaf(
//                toPlace,
//                plugin.getLeafCache()
//                /*,
//                false,
//                new HashSet<>()*/
//        );
    }

    private void placeLeaf(Block block) {
//        Bukkit.broadcastMessage("Placed leaf");
//        if (!(block.getBlockData() instanceof final Leaves leaves)) {
//            Bukkit.broadcastMessage("Not leaves");
//            return;
//        }
//        final LeafCache leafCache = this.plugin.getLeafCache();
//        final Location location = block.getLocation();
//        FakeLeafState state = leafCache.getAt(location);
//        if (state == null) state = leafCache.createAtOrGetAndSet(location, block.getType());
////        LeafUtil.updateDistances(leafCache, block, state);
//        LeafUpdater.doUpdate(this.plugin, Map.of(location, block.getBlockData()));
    }

//    @EventHandler
//    public void onBlockPlace(BlockPlaceEvent event) {
//        if (!Tag.LOGS.isTagged(event.getBlock().getType())) return;
//        LeafUpdater.doUpdate(this.plugin, Map.of(event.getBlock().getLocation(), event.getBlock().getBlockData()));
//    }

    private void handleInfoClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        final Block clicked = event.getClickedBlock();
        if (clicked == null) return;
        final ItemStack itemStack = event.getItem();
        if (itemStack == null) return;
        if (!Config.DEBUG_TOOL_ID.equals(PDCUtil.getLeafDataItemId(itemStack))) return;

        final Player player = event.getPlayer();
        if (!(clicked.getBlockData() instanceof final Leaves leaves)) {
            if (this.plugin.getLeafCache().isLogAt(clicked.getLocation())) {
                player.sendMessage("Log");
            } else if (this.config.isLogBlock(clicked.getBlockData())) {
                player.sendMessage("Not log but should be");
            }
            return;
        }
//        player.sendMessage(this.plugin.getLeafCache().getAt(
//                        new Position2D(clicked.getWorld().getUID(), clicked.getChunk().getX(), clicked.getChunk().getZ()),
//                        new Position(ChunkUtil.getCoordInChunk(clicked.getX()), clicked.getY(), ChunkUtil.getCoordInChunk(clicked.getZ()))
//                ).toString()
//        );
        final FakeLeafState fakeLeafState = this.plugin.getLeafCache().getAt(
                new Position2D(clicked.getWorld().getUID(), clicked.getChunk().getX(), clicked.getChunk().getZ()),
                new Position(ChunkUtil.getCoordInChunk(clicked.getX()), clicked.getY(), ChunkUtil.getCoordInChunk(clicked.getZ()))
        );
        if (fakeLeafState == null) {
            player.sendMessage("Actual distance: " + leaves.getDistance() + " actual persistence: " + leaves.isPersistent());
            player.sendMessage("The fake leaf state was not found");
            return;
        }
        player.sendMessage("Actual distance: " + fakeLeafState.actualDistance() + " actual persistence: " + fakeLeafState.actuallyPersistent() + " " +
                "server distance: " + leaves.getDistance() + " server persistence: " + leaves.isPersistent());
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
        final Position position = new Position(ChunkUtil.getCoordInChunk(location.getBlockX()), location.getBlockY(), ChunkUtil.getCoordInChunk(location.getBlockZ()));
        final FakeLeafState fakeLeafState = this.plugin.getLeafCache().getAt(chunkPos, position);
        if (fakeLeafState == null) return;
        final LeafItem leafItem = this.plugin.config().getByState(fakeLeafState);
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

//    @Nullable
//    private FakeLeafState removeBlock(Block block, Collection<? extends Player> players) {
//        final Chunk chunk = block.getChunk();
//        final Position2D chunkPos = new Position2D(block.getWorld().getUID(), chunk.getX(), chunk.getZ());
//        final Position position = new Position(ChunkUtil.getCoordInChunk(block.getX()), block.getY(), ChunkUtil.getCoordInChunk(block.getZ()));
//        final var previous = this.plugin.getLeafCache().remove(chunkPos, position);
//        if (!(block.getBlockData() instanceof Leaves)) return null;
//        final CustomBlockData blockData = new CustomBlockData(block, this.plugin);
//        blockData.remove(PDCUtil.PERSISTENCE_KEY);
//        blockData.remove(PDCUtil.DISTANCE_KEY);
//        Bukkit.getScheduler().runTaskLaterAsynchronously(
//                this.plugin,
//                () -> {
//                    PacketHelper.sendBlock(
//                            block.getWorld().getUID(),
//                            block.getX(),
//                            block.getY(),
//                            block.getZ(),
//                            WrappedBlockState.getDefaultState(
//                                    PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
//                                    StateTypes.AIR
//                            ),
//                            players.toArray(new Player[0])
//                    );
//                },
//                1
//        );
//        return previous;
//    }

//    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
//    public void onTreeGrow(StructureGrowEvent event) {
//        for (BlockState block : event.getBlocks()) {
//            final Material material = block.getType();
//            if (!Tag.LEAVES.isTagged(material)) continue;
//            final FakeLeafState state = this.plugin.config().getDefaultState(material);
//            final CustomBlockData customBlockData = new CustomBlockData(block.getBlock(), this.plugin);
//            customBlockData.set(PDCUtil.PERSISTENCE_KEY, PersistentDataType.BYTE, state.state().isPersistent() ? (byte) 1 : (byte) 0);
//            customBlockData.set(PDCUtil.DISTANCE_KEY, PersistentDataType.BYTE, (byte) state.state().getDistance());
//            customBlockData.set(PDCUtil.ACTUAL_PERSISTENCE_KEY, PersistentDataType.BYTE, state.actuallyPersistent() ? (byte) 1 : (byte) 0);
//            this.plugin.getLeafCache().addData(new Position2D(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()), new Position(ChunkUtil.getCoordInChunk(block.getX()), block.getY(), ChunkUtil.getCoordInChunk(block.getZ())), state);
//        }
//    }

//    @EventHandler
//    public void onBlockBreak(BlockBreakEvent event) {
//        final Block block = event.getBlock();
//        if (!this.plugin.config().isLogBlock(block) && !Tag.LEAVES.isTagged(block.getType())) {
//            Bukkit.broadcastMessage("Not correct type: " + block);
//            return;
//        }
//        Bukkit.broadcastMessage("What the heck");
//        final Location location = block.getLocation();
//        final FakeLeafState fakeLeafState = this.plugin.getLeafCache().getAt(
//                new Position2D(location.getWorld().getUID(), location.getChunk().getX(), location.getChunk().getZ()),
//                new Position(ChunkUtil.getCoordInChunk(location.getBlockX()), location.getBlockY(), ChunkUtil.getCoordInChunk(location.getBlockZ()))
//        );
//        if (fakeLeafState == null) return;
//        Bukkit.broadcastMessage("Checking leaves");
//        LeafUtil.checkIfDisablePersistence(
//                block,
//                fakeLeafState,
//                this.plugin.getLeafCache(),
//                this.plugin.config(),
//                false,
//                new HashSet<>()
//        );
//
//    }
//
//    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
//    public void onRemove(BlockBreakEvent event) {
//        if (event instanceof LeafBlockBreakEvent) return;
//        final Block block = event.getBlock();
//        if (!(block.getBlockData() instanceof Leaves)) return;
//        Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, () -> {
//            final var state = this.removeBlock(block, Bukkit.getOnlinePlayers());
////            if (state == null) return;
////        event.setCancelled(true);
////            if (!(block.getBlockData() instanceof Leaves leaves)) return;
////            leaves.setPersistent(state.isPersistent());
////            leaves.setDistance(state.getDistance());
////            block.setBlockData(leaves);
////            final var breakEvent = new LeafBlockBreakEvent(block, event.getPlayer());
////            Bukkit.getPluginManager().callEvent(breakEvent);
////            if (breakEvent.isCancelled()) return;
////            block.setType(Material.AIR);
//        }, 1);
//    }

    // paper
//    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
//    public void onRemove(BlockDestroyEvent event) {
//        if (event instanceof LeafBlockDestroyEvent) return;
//        final Block block = event.getBlock();
//        if (!(block.getBlockData() instanceof Leaves)) return;
//        final var state = this.removeBlock(block, Bukkit.getOnlinePlayers());
//        if (state == null) return;
//        event.setCancelled(true);
//        if (!(block.getBlockData() instanceof Leaves leaves)) return;
//        leaves.setPersistent(state.isPersistent());
//        leaves.setDistance(state.getDistance());
//        block.setBlockData(leaves);
//        final var breakEvent = new LeafBlockDestroyEvent(block, event.getNewState(), event.willDrop());
//        Bukkit.getPluginManager().callEvent(breakEvent);
//        if (breakEvent.isCancelled()) return;
//        block.setType(Material.AIR);
//    }

//    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
//    public void onRemove(BlockExplodeEvent event) {
//        if (event instanceof LeafBlockExplodeEvent) return;
//        boolean changed = false;
//        for (Block block : event.blockList()) {
//            if (!(block.getBlockData() instanceof Leaves)) continue;
//            final var state = this.removeBlock(block, event.getBlock().getWorld().getPlayers());
//            if (state == null) continue;
//            if (!(block.getBlockData() instanceof Leaves leaves)) continue;
//            leaves.setPersistent(state.state().isPersistent());
//            leaves.setDistance(state.state().getDistance());
//            block.setBlockData(leaves);
//            changed = true;
//        }
//        if (!changed) return;
//        event.setCancelled(true);
//        final var explodeEvent = new LeafBlockExplodeEvent(event.getBlock(), event.blockList(), event.getYield());
//        Bukkit.getPluginManager().callEvent(explodeEvent);
//        if (explodeEvent.isCancelled()) return;
//        for (Block b : event.blockList()) {
//            b.setType(Material.AIR);
//        }
//    }
//
//    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
//    public void onRemove(EntityExplodeEvent event) {
//        if (event instanceof LeafEntityExplodeEvent) return;
//        boolean changed = false;
//        for (Block block : event.blockList()) {
//            if (!(block.getBlockData() instanceof Leaves)) continue;
//            final var state = this.removeBlock(block, event.getEntity().getWorld().getPlayers());
//            if (state == null) continue;
//            if (!(block.getBlockData() instanceof Leaves leaves)) continue;
//            leaves.setPersistent(state.state().isPersistent());
//            leaves.setDistance(state.state().getDistance());
//            block.setBlockData(leaves);
//            changed = true;
//        }
//        if (!changed) return;
//        event.setCancelled(true);
//        final var explodeEvent = new LeafEntityExplodeEvent(event.getEntity(), event.getLocation(), event.blockList(), event.getYield());
//        Bukkit.getPluginManager().callEvent(explodeEvent);
//        if (explodeEvent.isCancelled()) return;
//        for (Block b : event.blockList()) {
//            b.setType(Material.AIR);
//        }
//    }

//    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
//    public void onRemove(LeavesDecayEvent event) {
//        if (event instanceof LeafLeavesDecayEvent) return;
//        final Block block = event.getBlock();
//        if (!(block.getBlockData() instanceof Leaves)) return;
////        final var state = this.removeBlock(block, Bukkit.getOnlinePlayers());
//        final var state = this.plugin.getLeafCache().remove(block.getLocation());
//        if (state == null) return;
//        event.setCancelled(true);
//        if (!(block.getBlockData() instanceof Leaves leaves)) return;
//        leaves.setPersistent(state.state().isPersistent());
//        leaves.setDistance(state.state().getDistance());
//        block.setBlockData(leaves);
//        final var breakEvent = new LeafLeavesDecayEvent(block);
//        Bukkit.getPluginManager().callEvent(breakEvent);
//        if (breakEvent.isCancelled()) return;
//        block.setType(Material.AIR);
//    }

    // paper
//    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
//    public void onRemove(BlockBreakBlockEvent event) {
//        if (event instanceof LeafBlockBreakBlockEvent) return;
//        final Block block = event.getBlock();
//        if (!(block.getBlockData() instanceof Leaves)) return;
//        final var state = this.removeBlock(block, Bukkit.getOnlinePlayers());
//        if (state == null) return;
//        if (!(block.getBlockData() instanceof Leaves leaves)) return;
//        leaves.setPersistent(state.isPersistent());
//        leaves.setDistance(state.getDistance());
//        block.setBlockData(leaves);
//        final var breakEvent = new LeafLeavesDecayEvent(block);
//        Bukkit.getPluginManager().callEvent(breakEvent);
//        if (breakEvent.isCancelled()) return;
//        block.setType(Material.AIR);
//    }

    // paper

//    private static class LeafBlockDestroyEvent extends BlockDestroyEvent {
//
//        public LeafBlockDestroyEvent(@NotNull Block block, @NotNull BlockData newState, boolean willDrop) {
//            super(block, newState, willDrop);
//        }
//
//    }
//
//    private static class LeafBlockBreakBlockEvent extends BlockBreakBlockEvent {
//
//        public LeafBlockBreakBlockEvent(@NotNull Block block, @NotNull Block source, @NotNull List<ItemStack> drops) {
//            super(block, source, drops);
//        }
//
//    }

}
