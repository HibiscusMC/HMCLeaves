package com.hibiscusmc.hmcleaves.paper.listener;

import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.block.BlockPlaceData;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlock;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlockState;
import com.hibiscusmc.hmcleaves.paper.config.LeavesConfig;
import com.hibiscusmc.hmcleaves.paper.packet.PacketUtil;
import com.hibiscusmc.hmcleaves.paper.util.PlayerUtils;
import com.hibiscusmc.hmcleaves.paper.util.WorldUtil;
import com.hibiscusmc.hmcleaves.paper.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.paper.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorld;
import com.hibiscusmc.hmcleaves.paper.world.Position;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class BlockPlaceListener extends CustomBlockListener {

    public BlockPlaceListener(HMCLeaves plugin) {
        super(plugin);
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

        final Position position;
        if (block.isReplaceable() || block.getType().isAir() || Tag.REPLACEABLE.isTagged(block.getType())) {
            position = WorldUtil.convertLocation(block.getLocation());
        } else {
            final Block relativeBlock = block.getRelative(event.getBlockFace());
            position = WorldUtil.convertLocation(relativeBlock.getLocation());
        }
        final ChunkPosition chunkPosition = position.toChunkPosition();
        final LeavesChunk clickChunk = leavesWorld.getChunk(WorldUtil.convertLocation(block.getLocation()).toChunkPosition());
        if (this.config.isDebugItem(itemStack) && clickChunk != null) {
            final CustomBlockState customBlock = clickChunk.getBlock(WorldUtil.convertLocation(block.getLocation()));
            if (customBlock != null && player.hasPermission(LeavesConfig.PLACE_DECAYABLE_PERMISSION)) {
                this.config.sendDebugInfo(player, customBlock, block.getBlockData());
            }
            event.setCancelled(true);
            return;
        }

        final CustomBlock customBlock = this.config.getBlockFromItem(itemStack);
        if (customBlock == null) {
            return;
        }
        final Block placeBlock = WorldUtil.convertPosition(world, position).getBlock();
        if (!placeBlock.getType().isAir() && !placeBlock.isReplaceable() && !Tag.REPLACEABLE.isTagged(placeBlock.getType())) {
            event.setCancelled(true);
            return;
        }
        if (!player.isSneaking() && block.getBlockData().getMaterial().isInteractable()) {
            return;
        }
        final Location placeLocation = WorldUtil.convertPosition(world, position);
        final Location center = placeLocation.clone().add(0.5, 0.5, 0.5);
        if (!world.getNearbyEntities(center, 0.5, 0.5, 0.5, LivingEntity.class::isInstance).isEmpty()) {
            event.setCancelled(true);
            return;
        }
        if (!customBlock.placeBlock(this.config, new BlockPlaceData(player, placeLocation, event.getClickedBlock(), event.getBlockFace()), customBlockState -> {
            if (player.getGameMode() != GameMode.CREATIVE) {
                final int amount = itemStack.getAmount();
                if (amount <= 1) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    itemStack.setAmount(amount - 1);
                }
            }
            leavesWorld.editInsertChunk(chunkPosition, leavesChunk -> leavesChunk.setBlock(position, customBlockState));
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                PacketUtil.sendArmSwing(player);
                PacketUtil.sendSingleBlockChange(customBlockState.getBlockState(), position, PlayerUtils.getNearbyPlayers(world.getUID(), chunkPosition));
            });
        })) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        final Block block = event.getBlock();
        final World world = block.getWorld();
        if (!this.config.isWorldWhitelisted(world.getName())) {
            return;
        }
        final ItemStack itemStack = event.getItemInHand();
        final CustomBlock customBlock = this.config.getBlockFromItem(itemStack);
        if (customBlock == null) {
            return;
        }
        event.setCancelled(true);
    }

}
