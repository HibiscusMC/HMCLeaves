package io.github.fisher2911.hmcleaves.listener;

import com.jeff_media.customblockdata.CustomBlockData;
import io.github.fisher2911.hmcleaves.Config;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.HMCLeavesAPI;
import io.github.fisher2911.hmcleaves.LeafData;
import io.github.fisher2911.hmcleaves.LeafItem;
import io.github.fisher2911.hmcleaves.nms.FakeLeafData;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import io.github.fisher2911.hmcleaves.util.PositionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;

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
        final World world = toPlace.getWorld();
        if (!(world.getNearbyEntities(toPlace.getLocation().clone().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5, LivingEntity.class::isInstance).isEmpty()))
            return;
        final ItemStack itemStack = event.getItem();
        if (itemStack == null) return;
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return;
        final String id = PDCUtil.getLeafDataItemId(itemStack);/* itemMeta.getPersistentDataContainer().get(PDCUtil.ITEM_KEY, PersistentDataType.STRING);*/
        if (id == null && !Tag.LEAVES.isTagged(itemStack.getType())) return;
        final Chunk chunk = toPlace.getChunk();
        final Position2D chunkPos = new Position2D(toPlace.getWorld().getUID(), chunk.getX(), chunk.getZ());
        final Position position = new Position(PositionUtil.getCoordInChunk(toPlace.getX()), toPlace.getY(), PositionUtil.getCoordInChunk(toPlace.getZ()));
        event.getPlayer().sendMessage("Is chunk loaded: " + PDCUtil.hasLeafData(chunk.getPersistentDataContainer()));
        event.getPlayer().sendMessage("Total custom blocks: " + CustomBlockData.getBlocksWithCustomData(this.plugin, chunk).size());
        final LeafItem leafItem = plugin.config().getItem(id);
        LeafData leafData = leafItem != null ? leafItem.leafData() : PDCUtil.getLeafData(itemMeta.getPersistentDataContainer());
//        final FakeLeafData defaultFakeLeafData = this.plugin.config().getDefaultData();
//        if (leafData == null) {
//            if (!Tag.LEAVES.isTagged(itemStack.getType())) return;
//            leafData = new LeafData(itemStack.getType(), defaultFakeLeafData.fakeDistance(), defaultFakeLeafData.fakePersistence(), true);
//        }
//        final FakeLeafData fakeLeafData = new FakeLeafData(
//                leafData.fakeDistance(),
//                leafData.fakePersistence(),
//                7,
//                leafData.actuallyPersistent()
//        );
//        this.plugin.getLeafCache().addData(chunkPos, position, fakeLeafData);
//        event.getPlayer().sendMessage("§aPlaced leaf: " + fakeLeafData);
//        toPlace.setType(leafData.material(), true);
//        final LeafData finalLeafData = leafData;
//        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
//        CraftServer
//            final Leaves leaves = (Leaves) toPlace.getBlockData();
//            leaves.setDistance(finalLeafData.fakeDistance());
//            leaves.setPersistent(fakeLeafData.fakePersistence());
//            toPlace.setBlockData(leaves);
//            toPlace.getLocation().getBlock().getState().update(true, true);
//            Bukkit.broadcastMessage("§aUpdated leaf: " + fakeLeafData + " " + toPlace.getState().update());
//        }, 20);
        HMCLeavesAPI.getInstance().setLeafAt(toPlace.getLocation(), leafData, itemStack.getType());
//        final CustomBlockData customBlockData = new CustomBlockData(toPlace, this.plugin);
//        customBlockData.set(PDCUtil.PERSISTENCE_KEY, PersistentDataType.BYTE, leafData == null ? (byte) 1 : leafData.fakePersistence() ? (byte) 1 : (byte) 0);
//        customBlockData.set(PDCUtil.DISTANCE_KEY, PersistentDataType.BYTE, (byte) leafData.fakeDistance());
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) itemStack.setAmount(itemStack.getAmount() - 1);
//        event.setCancelled(true);
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
        final FakeLeafData fakeLeafData = this.plugin.getLeafCache().getAt(
                new Position2D(clicked.getWorld().getUID(), clicked.getChunk().getX(), clicked.getChunk().getZ()),
                new Position(PositionUtil.getCoordInChunk(clicked.getX()), clicked.getY(), PositionUtil.getCoordInChunk(clicked.getZ()))
        );
        if (fakeLeafData == null) {
            player.sendMessage("Actual distance: " + leaves.getDistance() + " actual persistence: " + leaves.isPersistent());
            player.sendMessage("The fake leaf state was not found");
            return;
        }
        player.sendMessage("Leaves distance: " + leaves.getDistance() + " leaves persistence: " + leaves.isPersistent());
        player.sendMessage("Server distance: " + fakeLeafData.actualDistance() + " Server persistence: " + fakeLeafData.actualPersistence() + " " +
                "fakeDistance: " + fakeLeafData.fakeDistance() + " fakePersistence: " + fakeLeafData.fakePersistence());
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
        final FakeLeafData fakeLeafData = this.plugin.getLeafCache().getAt(chunkPos, position);
        if (fakeLeafData == null) return;
        final LeafItem leafItem = this.plugin.config().getByFakeLeafData(fakeLeafData);
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


    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        final World world = event.getBlock().getWorld();
        final Block bLock = event.getBlock();
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            this.plugin.getLeafCache().remove(world.getUID(), bLock.getX(), bLock.getY(), bLock.getZ());
        }, 3);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlocksExplode(BlockExplodeEvent event) {
        final World world = event.getBlock().getWorld();
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            for (Block block : event.blockList()) {
                this.plugin.getLeafCache().remove(world.getUID(), block.getX(), block.getY(), block.getZ());
            }
        }, 3);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event) {
        final World world = event.getEntity().getWorld();
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            for (Block block : event.blockList()) {
                this.plugin.getLeafCache().remove(world.getUID(), block.getX(), block.getY(), block.getZ());
            }
        }, 3);
    }

}
