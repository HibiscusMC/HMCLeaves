package io.github.fisher2911.hmcleaves.listener;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.customblockdata.events.CustomBlockDataRemoveEvent;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.LeafItem;
import io.github.fisher2911.hmcleaves.packet.PacketHelper;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import io.github.fisher2911.hmcleaves.util.PositionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.List;

public class PlaceListener implements Listener {

    private final HMCLeaves plugin;

    public PlaceListener(HMCLeaves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        final Block toPlace = event.getClickedBlock().getRelative(event.getBlockFace());
        final ItemStack itemStack = event.getItem();
        if (itemStack == null) return;
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return;
        final String id = itemMeta.getPersistentDataContainer().get(PDCUtil.ITEM_KEY, PersistentDataType.STRING);
        if (id == null) return;
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
        PacketHelper.sendLeaf(toPlace.getX(), toPlace.getY(), toPlace.getZ(), defaultState);
        final Chunk chunk = toPlace.getChunk();
        final Position2D chunkPos = new Position2D(toPlace.getWorld().getUID(), chunk.getX(), chunk.getZ());
        final Position position = new Position(PositionUtil.getCoordInChunk(toPlace.getX()), toPlace.getY(), PositionUtil.getCoordInChunk(toPlace.getZ()));
        this.plugin.getLeafCache().addData(chunkPos, position, defaultState);
        final CustomBlockData customBlockData = new CustomBlockData(toPlace, this.plugin);
        customBlockData.set(PDCUtil.PERSISTENCE_KEY, PersistentDataType.BYTE, leafItem.persistent() ? (byte) 1 : (byte) 0);
        customBlockData.set(PDCUtil.DISTANCE_KEY, PersistentDataType.BYTE, (byte) leafItem.distance());
        itemStack.setAmount(itemStack.getAmount() - 1);
    }

    private void removeBlock(Block block, Collection<? extends Player> players) {
        if (!Tag.LEAVES.isTagged(block.getType())) return;
        final Chunk chunk = block.getChunk();
        final Position2D chunkPos = new Position2D(block.getWorld().getUID(), chunk.getX(), chunk.getZ());
        final Position position = new Position(PositionUtil.getCoordInChunk(block.getX()), block.getY(),PositionUtil.getCoordInChunk(block.getZ()));
        this.plugin.getLeafCache().remove(chunkPos, position);
        PacketHelper.sendBlock(block.getX(), block.getY(), block.getZ(), this.plugin.config().getDefaultState(block.getType()).clone(), players.toArray(new Player[0]));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onTreeGrow(StructureGrowEvent event) {
        for (BlockState block : event.getBlocks()) {
            final Material material = block.getType();
            if (!Tag.LEAVES.isTagged(material)) continue;
            final var state = this.plugin.config().getDefaultState(material).clone();
            PacketHelper.sendLeaf(block.getX(), block.getY(), block.getZ(), state);
            final CustomBlockData customBlockData = new CustomBlockData(block.getBlock(), this.plugin);
            customBlockData.set(PDCUtil.PERSISTENCE_KEY, PersistentDataType.BYTE, state.isPersistent() ? (byte) 1 : (byte) 0);
            customBlockData.set(PDCUtil.DISTANCE_KEY, PersistentDataType.BYTE, (byte) state.getDistance());
            this.plugin.getLeafCache().addData(new Position2D(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()), new Position(PositionUtil.getCoordInChunk(block.getX()), block.getY(), PositionUtil.getCoordInChunk(block.getZ())), state);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCustomBlockBreak(CustomBlockDataRemoveEvent event) {
        final Block block = event.getBlock();
        this.removeBlock(block, Bukkit.getOnlinePlayers());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onRemove(BlockBreakEvent event) {
        final Block block = event.getBlock();
        this.removeBlock(block, List.of(event.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onRemove(BlockDestroyEvent event) {
        final Block block = event.getBlock();
        this.removeBlock(block, Bukkit.getOnlinePlayers());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onRemove(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            this.removeBlock(block, Bukkit.getOnlinePlayers());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onRemove(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            this.removeBlock(block, Bukkit.getOnlinePlayers());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onRemove(LeavesDecayEvent event) {
        final Block block = event.getBlock();
        this.removeBlock(block, Bukkit.getOnlinePlayers());
    }

}
