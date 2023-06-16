/*
 *
 *  *     HMCLeaves
 *  *     Copyright (C) 2022  Hibiscus Creative Studios
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU General Public License
 *  *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package io.github.fisher2911.hmcleaves.listener;

import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.cache.BlockCache;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.data.LeafData;
import io.github.fisher2911.hmcleaves.data.LogData;
import io.github.fisher2911.hmcleaves.data.SaplingData;
import io.github.fisher2911.hmcleaves.packet.BlockBreakManager;
import io.github.fisher2911.hmcleaves.packet.PacketUtils;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class InteractionListener implements Listener {

    private final HMCLeaves plugin;
    private final LeavesConfig leavesConfig;
    private final BlockCache blockCache;

    public InteractionListener(HMCLeaves plugin) {
        this.plugin = plugin;
        this.leavesConfig = plugin.getLeavesConfig();
        this.blockCache = plugin.getBlockCache();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        final World world = event.getPlayer().getWorld();
        if (!this.leavesConfig.isWorldWhitelisted(world)) return;
        final ItemStack clickedWith = event.getItem();
        if (clickedWith == null) return;
        final Block block = event.getClickedBlock();
        if (block == null) return;
        final Location placeLocation;
        if (block.getType() == Material.GRASS) {
            placeLocation = block.getLocation();
        } else {
            placeLocation = block.getRelative(event.getBlockFace()).getLocation();
        }
        final Player player = event.getPlayer();
        if (this.checkWaterlog(player, block, clickedWith)) {
            return;
        }
        final Material placeLocationType = placeLocation.getBlock().getType();

        if (!placeLocationType.isAir() &&
                placeLocationType != Material.GRASS &&
                placeLocationType != Material.WATER &&
                placeLocationType != Material.LAVA
        ) {
            return;
        }
        final Axis axis = this.axisFromBlockFace(event.getBlockFace());
        final BlockData blockData = this.leavesConfig.getBlockData(clickedWith, axis);
        if (this.doDebugTool(clickedWith, event.getPlayer(), block)) {
            event.setCancelled(true);
            return;
        }
        if (this.checkStripLog(player, block, clickedWith)) return;
        if (blockData == null) return;
        if (block.getType().isInteractable() && !player.isSneaking()) return;
        if (!(blockData instanceof SaplingData) && !(world.getNearbyEntities(placeLocation.clone().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5, LivingEntity.class::isInstance).isEmpty())) {
            event.setCancelled(true);
            return;
        }
        if (
                blockData instanceof SaplingData &&
                        !Tag.DIRT.isTagged(placeLocation.clone().subtract(0, 1, 0).getBlock().getType())
        ) {
            event.setCancelled(true);
            return;
        }
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            final Block placedBlock = placeLocation.getBlock();
            final BlockState previousState = placedBlock.getState();
            placedBlock.setType(blockData.worldBlockType());
            final BlockPlaceEvent blockPlaceEvent = createEvent(placedBlock, previousState, block, clickedWith, player, true, event.getHand());
            Bukkit.getPluginManager().callEvent(blockPlaceEvent);
            if (blockPlaceEvent.isCancelled()) {
                previousState.update(true, false);
                return;
            }
            if (player.getGameMode() != GameMode.CREATIVE) {
                clickedWith.setAmount(clickedWith.getAmount() - 1);
            }
            final boolean waterlogged = this.isStateWaterSource(previousState);
            final Position position = Position.fromLocation(placeLocation);
            if (waterlogged && blockData instanceof final LeafData leafData) {
                this.blockCache.addBlockData(position, leafData.waterlog(waterlogged));
            } else {
                this.blockCache.addBlockData(position, blockData);
            }
            PacketUtils.sendArmSwing(player, List.of(player));
            final Sound sound = blockData.placeSound();
            if (sound != null) {
                world.playSound(placeLocation, sound, 1, 1);
            }
            if (placedBlock.getBlockData() instanceof final Leaves leaves) {
                if (!(blockData instanceof final LeafData leafData)) {
                    return;
                }
                if (player.getGameMode() != GameMode.CREATIVE || leafData.worldPersistence() || this.leavesConfig.isOnlyFollowWorldPersistenceIfConnectedToLog()) {
                    leaves.setPersistent(true);
                }
                leaves.setDistance(this.getDistance(placeLocation));
                if (leaves.isPersistent() && leaves.getDistance() < 7 && this.leavesConfig.isOnlyFollowWorldPersistenceIfConnectedToLog()) {
                    leaves.setPersistent(false);
                }
                if (waterlogged && leaves instanceof final Waterlogged waterloggedData) {
                    waterloggedData.setWaterlogged(true);
                }
                placedBlock.setBlockData(leaves, true);
                return;
            }
            if (blockData instanceof final LogData logData && placedBlock.getBlockData() instanceof final Orientable orientable) {
                orientable.setAxis(logData.axis());
                placedBlock.setBlockData(orientable, true);
            }
        }, 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event instanceof LeafPlaceEvent || event instanceof LogPlaceEvent) return;
        final Block block = event.getBlock();
        if (!this.leavesConfig.isWorldWhitelisted(block.getWorld())) return;
        final Material material = block.getType();
        final Position position = Position.fromLocation(block.getLocation());
        if (block.getBlockData() instanceof Leaves) {
            final BlockData blockData = this.leavesConfig.getDefaultLeafData(material);
            if (!(blockData instanceof final LeafData leafData)) return;
            final BlockState replacedState = event.getBlockReplacedState();
            this.blockCache.addBlockData(position,
                    leafData.waterlog(this.isStateWaterSource(replacedState))
            );
            return;
        }
        if (Tag.LOGS.isTagged(material)) {
            final Orientable orientable = (Orientable) block.getBlockData();
            final BlockData blockData = this.leavesConfig.getDefaultLogData(material, orientable.getAxis());
            if (blockData == null) return;
            this.blockCache.addBlockData(position, blockData);
            return;
        }
        if (Tag.SAPLINGS.isTagged(material)) {
            final BlockData blockData = this.leavesConfig.getDefaultSaplingData(material);
            if (blockData == null) return;
            this.blockCache.addBlockData(position, blockData);
        }
    }

    /**
     * Logs should only drop using the {@link io.github.fisher2911.hmcleaves.packet.BlockBreakManager}
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        if (event instanceof BlockBreakManager.LeavesBlockBreakEvent) return;
        if (!this.leavesConfig.isWorldWhitelisted(event.getBlock().getWorld())) return;
        final Location location = event.getBlock().getLocation();
        final Position position = Position.fromLocation(location);
        final BlockData blockData = this.blockCache.getBlockData(position);
        if (blockData instanceof final SaplingData saplingData) {
            final ItemStack itemStack = this.leavesConfig.getItemStack(saplingData.id());
            if (itemStack == null) return;
            event.setDropItems(false);
            final World world = event.getBlock().getWorld();
            this.blockCache.removeBlockData(position);
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                world.dropItem(location.clone().add(0.5, 0, 0.5), itemStack);
            }, 1);
            return;
        }
        if (!(blockData instanceof LogData)) return;
        event.setCancelled(true);
    }

    private static final Set<BlockFace> BLOCK_RELATIVE_FACES = Set.of(
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    );

    private int getDistance(Location location) {
        final Block block = location.getBlock();
        int minDistance = 7;
        for (BlockFace blockFace : BLOCK_RELATIVE_FACES) {
            final Block relative = block.getRelative(blockFace);
            // for some reason leaf distance isn't updating when placing near a log or leaf
            if (Tag.LOGS.isTagged(relative.getType())) {
                return 1;
            }
            if (Tag.LEAVES.isTagged(relative.getType())) {
                minDistance = Math.min(minDistance, ((Leaves) relative.getBlockData()).getDistance());
            }
        }
        return Math.min(minDistance + 1, 7);
    }

    private boolean doDebugTool(ItemStack itemStack, Player player, Block clicked) {
        if (!LeavesConfig.DEBUG_TOOL_ID.equalsIgnoreCase(PDCUtil.getItemId(itemStack))) {
            return false;
        }
        final BlockData blockData = this.blockCache.getBlockData(Position.fromLocation(clicked.getLocation()));
        if (blockData instanceof final SaplingData saplingData && clicked.getBlockData() instanceof final Sapling sapling) {
            player.sendMessage(saplingData.id() + " displayStage: " + saplingData.getNewState().getStage() + " realStage: " + sapling.getStage() + " realBlockType: " + saplingData.worldBlockType() + " : " + clicked.getType());
            return true;
        }
        if (blockData instanceof final LogData logData) {
            player.sendMessage(logData.id() + " Log type: " + " stripped: " + logData.stripped() + " realBlockType:" + logData.worldBlockType() + " : " + clicked.getType());
            return true;
        }
        if (blockData instanceof final LeafData leafData && clicked.getBlockData() instanceof final Leaves leaves) {
            player.sendMessage("Display distance: " + leafData.displayDistance() + " Display persistence: " + leafData.displayPersistence() + " " +
                    "server distance: " + leaves.getDistance() + " server persistence: " + leaves.isPersistent() + " waterlogged: " + leafData.waterlogged());
            return true;
        }
        if (blockData == BlockData.EMPTY) {
            player.sendMessage("The fake block data was not found");
            return true;
        }
        player.sendMessage("The fake block data does not match the real block: " + blockData.getClass().getSimpleName());
        return true;
    }

    private static final Set<Material> AXES = Set.of(
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.GOLDEN_AXE,
            Material.DIAMOND_AXE,
            Material.NETHERITE_AXE
    );

    private boolean checkStripLog(Player player, Block block, ItemStack clickedWith) {
        if (!AXES.contains(clickedWith.getType())) return false;
        final Position position = Position.fromLocation(block.getLocation());
        final BlockData blockData = this.blockCache.getBlockData(position);
        if (!(blockData instanceof final LogData logData)) {
            return false;
        }
        if (logData.stripped()) {
            return false;
        }
        final LogData strippedData = logData.strip();
        this.blockCache.addBlockData(position, strippedData);
        block.setType(strippedData.worldBlockType());
        player.getWorld().playSound(block.getLocation(), Sound.ITEM_AXE_STRIP, 1, 1);
        return true;
    }

    private boolean checkWaterlog(Player player, Block block, ItemStack clickedWith) {
        final Position position = Position.fromLocation(block.getLocation());
        final BlockData blockData = this.blockCache.getBlockData(position);
        if (!(blockData instanceof final LeafData leafData)) {
            return false;
        }
        if (clickedWith.getType() == Material.WATER_BUCKET) {
            this.blockCache.addBlockData(position, leafData.waterlog(true));
            return true;
        }
        if (clickedWith.getType() == Material.BUCKET) {
            this.blockCache.addBlockData(position, leafData.waterlog(false));
            return true;
        }
        return false;
    }

    private Axis axisFromBlockFace(BlockFace face) {
        return switch (face) {
            case UP, DOWN -> Axis.Y;
            case NORTH, SOUTH -> Axis.X;
            case EAST, WEST -> Axis.Z;
            default -> throw new IllegalStateException("Unexpected value: " + face);
        };
    }

    private boolean isStateWaterSource(BlockState blockState) {
        return blockState.getBlockData() instanceof Levelled levelled &&
                levelled.getLevel() == 0 &&
                levelled.getMaterial() == Material.WATER;
    }

    private static class LogPlaceEvent extends BlockPlaceEvent {

        public LogPlaceEvent(@NotNull Block placedBlock, @NotNull BlockState replacedBlockState, @NotNull Block placedAgainst, @NotNull ItemStack itemInHand, @NotNull Player thePlayer, boolean canBuild, @NotNull EquipmentSlot hand) {
            super(placedBlock, replacedBlockState, placedAgainst, itemInHand, thePlayer, canBuild, hand);
        }

    }

    private static class LeafPlaceEvent extends BlockPlaceEvent {

        public LeafPlaceEvent(@NotNull Block placedBlock, @NotNull BlockState replacedBlockState, @NotNull Block placedAgainst, @NotNull ItemStack itemInHand, @NotNull Player thePlayer, boolean canBuild, @NotNull EquipmentSlot hand) {
            super(placedBlock, replacedBlockState, placedAgainst, itemInHand, thePlayer, canBuild, hand);
        }

    }

    private static class SaplingPlaceEvent extends BlockPlaceEvent {

        public SaplingPlaceEvent(@NotNull Block placedBlock, @NotNull BlockState replacedBlockState, @NotNull Block placedAgainst, @NotNull ItemStack itemInHand, @NotNull Player thePlayer, boolean canBuild, @NotNull EquipmentSlot hand) {
            super(placedBlock, replacedBlockState, placedAgainst, itemInHand, thePlayer, canBuild, hand);
        }

    }

    private static BlockPlaceEvent createEvent(@NotNull Block placedBlock, @NotNull BlockState replacedBlockState, @NotNull Block placedAgainst, @NotNull ItemStack itemInHand, @NotNull Player thePlayer, boolean canBuild, @NotNull EquipmentSlot hand) {
        if (Tag.LOGS.isTagged(placedBlock.getType())) {
            return new LogPlaceEvent(placedBlock, replacedBlockState, placedAgainst, itemInHand, thePlayer, canBuild, hand);
        }
        if (Tag.LEAVES.isTagged(placedBlock.getType())) {
            return new LeafPlaceEvent(placedBlock, replacedBlockState, placedAgainst, itemInHand, thePlayer, canBuild, hand);
        }
        if (Tag.SAPLINGS.isTagged(placedBlock.getType())) {
            return new SaplingPlaceEvent(placedBlock, replacedBlockState, placedAgainst, itemInHand, thePlayer, canBuild, hand);
        }
        throw new IllegalArgumentException("Block is not a log or leaf: " + placedBlock.getType());
    }

}
