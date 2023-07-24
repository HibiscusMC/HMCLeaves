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
import io.github.fisher2911.hmcleaves.data.AgeableData;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.data.CaveVineData;
import io.github.fisher2911.hmcleaves.data.LeafData;
import io.github.fisher2911.hmcleaves.data.LogData;
import io.github.fisher2911.hmcleaves.data.MineableData;
import io.github.fisher2911.hmcleaves.data.SaplingData;
import io.github.fisher2911.hmcleaves.hook.Hooks;
import io.github.fisher2911.hmcleaves.packet.BlockBreakManager;
import io.github.fisher2911.hmcleaves.packet.PacketUtils;
import io.github.fisher2911.hmcleaves.util.ChainedBlockUtil;
import io.github.fisher2911.hmcleaves.util.ItemUtil;
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
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.CaveVines;
import org.bukkit.block.data.type.CaveVinesPlant;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
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

    @EventHandler(/*ignoreCancelled = true, */priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        final World world = event.getPlayer().getWorld();
        if (!this.leavesConfig.isWorldWhitelisted(world)) return;
        final Block block = event.getClickedBlock();
        if (block == null) return;
        if (Hooks.getCustomBlockIdAt(block.getLocation().clone()) == null &&
                (event.useInteractedBlock() == Event.Result.DENY ||
                        event.useItemInHand() == Event.Result.DENY)
        ) {
            return;
        }
        final Position clickedPosition = Position.fromLocation(block.getLocation());
        final BlockData clickedBlockData = this.blockCache.getBlockData(clickedPosition);
        final ItemStack clickedWith = event.getItem();
        if (clickedWith != null && this.doDebugTool(clickedWith, event.getPlayer(), block)) {
            event.setCancelled(true);
            return;
        }
        final Player player = event.getPlayer();
        if (clickedBlockData instanceof final CaveVineData caveVineData && caveVineData.glowBerry() && !player.isSneaking()) {
            if (!(block.getBlockData() instanceof final CaveVinesPlant caveVines)) return;
            if (!caveVineData.shouldGrowBerries()) {
                event.setCancelled(true);
                return;
            }
            this.blockCache.addBlockData(clickedPosition, caveVineData.withGlowBerry(!caveVines.isBerries()));
            return;
        }
        if (clickedWith == null) return;
        final Location placeLocation;
        final BlockFace blockFace = event.getBlockFace();
        if (block.getType() == Material.GRASS) {
            placeLocation = block.getLocation();
        } else {
            placeLocation = block.getRelative(blockFace).getLocation();
        }
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
        final Axis axis = this.axisFromBlockFace(blockFace);
        final BlockData blockData = this.leavesConfig.getBlockData(clickedWith, axis);
        if (this.checkStripLog(player, block, clickedWith)) return;
        if ((
                block.getType().isInteractable() &&
                        Hooks.getCustomBlockIdAt(block.getLocation().clone()) == null
        ) && !player.isSneaking()) {
            if (!Tag.CAVE_VINES.isTagged(block.getType())) {
                return;
            }
        }
        if (blockData == null) return;
        if (!this.leavesConfig.canPlaceBlockAgainst(blockData, placeLocation.getBlock())) {
            event.setCancelled(true);
            return;
        }
        if (!(blockData instanceof SaplingData) && !(world.getNearbyEntities(placeLocation.clone().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5, LivingEntity.class::isInstance).isEmpty())) {
            event.setCancelled(true);
            return;
        }
        final Block placedBlock = placeLocation.getBlock();
        final BlockState previousState = placedBlock.getState();
        placedBlock.setType(blockData.worldBlockType(), false);
        final BlockPlaceEvent blockPlaceEvent = new HMCLeavesBlockDataPlaceEvent(
                placedBlock,
                previousState,
                block,
                clickedWith,
                player,
                true,
                event.getHand(),
                blockData
        );
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
        PacketUtils.sendArmSwing(player, List.of(player));
        if (waterlogged && blockData instanceof final LeafData leafData) {
            this.blockCache.addBlockData(position, leafData.waterlog(waterlogged));
        } else {
            this.blockCache.addBlockData(position, blockData);
        }
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
            if (leaves.isPersistent() && leaves.getDistance() < 7 && this.leavesConfig.isOnlyFollowWorldPersistenceIfConnectedToLog() && player.getGameMode() == GameMode.CREATIVE) {
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
            return;
        }
        if (blockData instanceof final CaveVineData caveVineData && placedBlock.getBlockData() instanceof final CaveVinesPlant caveVinesPlant) {
            placedBlock.setType(Material.AIR);
            caveVinesPlant.setBerries(caveVineData.glowBerry());
            placedBlock.setBlockData(caveVinesPlant, true);
            return;
        }
        if (LeavesConfig.AGEABLE_MATERIALS.contains(blockData.worldBlockType())) {
            placedBlock.setType(Material.AIR);
            placedBlock.setType(blockData.worldBlockType());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event instanceof HMCLeavesBlockDataPlaceEvent) return;
        final Block block = event.getBlock();
        if (!this.leavesConfig.isWorldWhitelisted(block.getWorld())) return;
        if (
                LeavesConfig.SHEAR_STOPS_GROWING_MATERIALS.contains(block.getType()) &&
                        event.getItemInHand().getType() == Material.SHEARS
        ) return;
        final Material material = block.getType();
        final Position position = Position.fromLocation(block.getLocation());
        final ItemStack itemInHand = event.getItemInHand();
        final BlockData itemStackBlockData = this.leavesConfig.getBlockData(itemInHand);
        if (block.getBlockData() instanceof Leaves) {
            final BlockData blockData;
            if (itemStackBlockData != null) {
                blockData = itemStackBlockData;
            } else {
                blockData = this.leavesConfig.getDefaultLeafData(material);
            }
            if (!(blockData instanceof final LeafData leafData)) return;
            if (!this.leavesConfig.canPlaceBlockAgainst(blockData, block)) {
                event.setCancelled(true);
                return;
            }
            final BlockState replacedState = event.getBlockReplacedState();
            this.blockCache.addBlockData(position,
                    leafData.waterlog(this.isStateWaterSource(replacedState))
            );
            return;
        }
        if (Tag.LOGS.isTagged(material)) {
            final Orientable orientable = (Orientable) block.getBlockData();
            final Axis axis = orientable.getAxis();
            BlockData blockData = this.leavesConfig.getBlockData(itemInHand, axis);
            if (blockData == null) {
                blockData = this.leavesConfig.getDefaultLogData(material, axis);
            }
            if (blockData == null) return;
            if (!this.leavesConfig.canPlaceBlockAgainst(blockData, block)) {
                event.setCancelled(true);
                return;
            }
            this.blockCache.addBlockData(position, blockData);
            return;
        }
        if (Tag.SAPLINGS.isTagged(material)) {
            final BlockData blockData;
            if (itemStackBlockData != null) {
                blockData = itemStackBlockData;
            } else {
                blockData = this.leavesConfig.getDefaultSaplingData(material);
            }
            if (blockData == null) return;
            if (!this.leavesConfig.canPlaceBlockAgainst(blockData, block)) {
                event.setCancelled(true);
                return;
            }
            this.blockCache.addBlockData(position, blockData);
        }
        if (Tag.CAVE_VINES.isTagged(material)) {
            final BlockData blockData;
            if (itemStackBlockData != null) {
                blockData = itemStackBlockData;
            } else {
                blockData = this.leavesConfig.getDefaultCaveVinesData(false);
            }
            if (blockData == null) return;
            if (!this.leavesConfig.canPlaceBlockAgainst(blockData, block)) {
                event.setCancelled(true);
                return;
            }
            this.blockCache.addBlockData(position, blockData);
            return;
        }
        if (LeavesConfig.AGEABLE_MATERIALS.contains(material)) {
            final BlockData blockData;
            if (itemStackBlockData != null) {
                blockData = itemStackBlockData;
            } else {
                blockData = this.leavesConfig.getDefaultAgeableData(material);
            }
            if (blockData == null) return;
            if (!this.leavesConfig.canPlaceBlockAgainst(blockData, event.getBlock())) {
                event.setCancelled(true);
                return;
            }
            this.blockCache.addBlockData(position, blockData);
        }
    }

    /**
     * Logs should only drop using the {@link io.github.fisher2911.hmcleaves.packet.BlockBreakManager}
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event instanceof BlockBreakManager.LeavesBlockBreakEvent) return;
        if (!this.leavesConfig.isWorldWhitelisted(event.getBlock().getWorld())) return;
        ChainedBlockUtil.handleBlockBreak(event.getBlock(), this.blockCache, this.leavesConfig);
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
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
        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        final Material material = heldItem.getType();
        if (ItemUtil.isQuickMiningTool(material)) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> PacketUtils.sendMiningFatigue(player));
        }
        if (!(blockData instanceof final MineableData mineableData) || mineableData.blockBreakModifier() == null) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onSwitchItemHeld(PlayerItemHeldEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        final ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        final ItemStack oldItem = player.getInventory().getItem(event.getPreviousSlot());
        if (newItem != null) {
            final Material material = newItem.getType();
            if (ItemUtil.isQuickMiningTool(material)) {
                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> PacketUtils.sendMiningFatigue(player));
                return;
            }
        }
        if (oldItem != null && player.getPotionEffect(PotionEffectType.SLOW_DIGGING) == null) {
            final Material material = oldItem.getType();
            if (ItemUtil.isQuickMiningTool(material)) {
                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> PacketUtils.removeMiningFatigue(player));
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockDamage(BlockDamageEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        final Block block = event.getBlock();
        final ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack == null) return;
        final BlockData blockData = this.blockCache.getBlockData(Position.fromLocation(block.getLocation()));
        if (blockData instanceof final MineableData mineableData && mineableData.blockBreakModifier() != null) {
            return;
        }
        final Material material = itemStack.getType();
        if (ItemUtil.isQuickMiningTool(material)) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> PacketUtils.removeMiningFatigue(player));
        }

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
            player.sendMessage(saplingData.id() + " displayStage: " + saplingData.getNewState(null).getStage() + " realStage: " + sapling.getStage() + " realBlockType: " + saplingData.worldBlockType() + " : " + clicked.getType());
            return true;
        }
        if (blockData instanceof final LogData logData) {
            player.sendMessage(logData.id() + " Log type: " + " stripped: " + logData.stripped() + " realBlockType: " + logData.worldBlockType() + " : " + logData.getNewState(null).getType().getName());
            return true;
        }
        if (blockData instanceof final LeafData leafData && clicked.getBlockData() instanceof final Leaves leaves) {
            player.sendMessage(leafData.id() + " Display distance: " + leafData.displayDistance() + " Display persistence: " + leafData.displayPersistence() + " " +
                    "server distance: " + leaves.getDistance() + " server persistence: " + leaves.isPersistent() + " waterlogged: " + leafData.waterlogged());
            return true;
        }
        if (blockData instanceof final CaveVineData caveVineData && clicked.getBlockData() instanceof final CaveVinesPlant caveVinesPlant) {
            player.sendMessage(caveVineData.id() + " worldMaterial: " + clicked.getType() + " Display berries: " + caveVineData.glowBerry() + " server berries: " + caveVinesPlant.isBerries() + " "
                    + "display age: " + caveVineData.getNewState(null).getAge() + " server age: " +
                    ((caveVinesPlant instanceof final CaveVines caveVines) ? caveVines.getAge() : "no age"));
            return true;
        }
        if (blockData instanceof final AgeableData ageableData) {
            player.sendMessage(blockData.id() + " Display age: " + ageableData.getAge() + " server age: " +
                    ((clicked.getBlockData()) instanceof final Ageable ageable ? ageable.getAge() : "no age"));
            return true;
        }
        if (blockData == BlockData.EMPTY) {
            player.sendMessage("The fake block data was not found: " + clicked.getType());
            return true;
        }
        player.sendMessage("The fake block data does not match the real block: " + blockData.getClass().getSimpleName() + " world block: " + clicked.getType());
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

    private static class HMCLeavesBlockDataPlaceEvent extends BlockPlaceEvent {

        private final BlockData blockData;

        public HMCLeavesBlockDataPlaceEvent(
                @NotNull Block placedBlock,
                @NotNull BlockState replacedBlockState,
                @NotNull Block placedAgainst,
                @NotNull ItemStack itemInHand,
                @NotNull Player thePlayer,
                boolean canBuild,
                @NotNull EquipmentSlot hand,
                BlockData blockData
        ) {
            super(placedBlock, replacedBlockState, placedAgainst, itemInHand, thePlayer, canBuild, hand);
            this.blockData = blockData;
        }

    }

}
