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

package io.github.fisher2911.hmcleaves.packet;

import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.cache.BlockCache;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.data.LogData;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.function.Supplier;

public class BlockBreakManager {

    private static final SplittableRandom RANDOM = new SplittableRandom();

    private final HMCLeaves plugin;
    private final BlockCache blockCache;
    private final LeavesConfig leavesConfig;
    private final Map<UUID, BlockBreakData> blockBreakDataMap;


    public BlockBreakManager(Map<UUID, BlockBreakData> blockBreakDataMap, HMCLeaves plugin) {
        this.blockBreakDataMap = blockBreakDataMap;
        this.plugin = plugin;
        this.blockCache = this.plugin.getBlockCache();
        this.leavesConfig = this.plugin.getLeavesConfig();
    }

    @Nullable
    public BlockBreakData getBlockBreakData(UUID uuid) {
        return this.blockBreakDataMap.get(uuid);
    }

    public void startBlockBreak(Player player, Position position, BlockData blockData) {
        final int blockBreakTime = this.calculateBlockBreakTimeInTicks(player, player.getInventory().getItemInMainHand());
//        final int period = (int) Math.ceil(blockBreakTime / (double) BlockBreakData.MAX_DAMAGE);
        final BlockBreakData blockBreakData = new BlockBreakData(
                RANDOM.nextInt(10_000, 20_000),
                blockData,
                player,
                position,
                player.getInventory().getItemInMainHand(),
                0,
                0,
                blockBreakTime,
                0,
                this.createScheduler(blockBreakTime, player.getUniqueId())
        );
        this.blockBreakDataMap.put(player.getUniqueId(), blockBreakData);
    }

    public void cancelBlockBreak(Player player) {
        final BlockBreakData blockBreakData = this.blockBreakDataMap.get(player.getUniqueId());
        if (blockBreakData == null) {
            return;
        }
        PacketUtils.sendBlockBreakAnimation(player, blockBreakData.getPosition().toLocation(), blockBreakData.getEntityId(), (byte) -1);
        blockBreakData.getBreakTask().cancel();
        this.blockBreakDataMap.remove(player.getUniqueId());
    }

    private BukkitTask createScheduler(int blockBreakTime, UUID uuid) {
        return Bukkit.getScheduler().runTaskTimer(this.plugin,
                    () -> {
                        final BlockBreakData blockBreakData = BlockBreakManager.this.blockBreakDataMap.get(uuid);
                        if (blockBreakData == null) {
                            return;
                        }
                        final Player player = blockBreakData.getBreaker();
                        if (blockBreakData.isBroken()) {
                            final Block block = blockBreakData.getPosition().toLocation().getBlock();
                            PacketUtils.sendBlockBreakAnimation(
                                    blockBreakData.getBreaker(),
                                    blockBreakData.getPosition().toLocation(),
                                    blockBreakData.getEntityId(),
                                    (byte) -1
                            );
                            PacketUtils.sendBlockBroken(
                                    player,
                                    blockBreakData.getPosition(),
                                    blockBreakData.getBlockData().sendBlockId()
                            );
                            BlockBreakManager.this.blockCache.removeBlockData(blockBreakData.getPosition());
                            block.setType(Material.AIR);
                            Supplier<ItemStack> itemStackSupplier = null;
                            if (blockBreakData.getBlockData() instanceof final LogData logData) {
                                if (logData.stripped()) {
                                    itemStackSupplier = BlockBreakManager.this.leavesConfig.getItem(logData.strippedLogId());
                                } else {
                                    itemStackSupplier = BlockBreakManager.this.leavesConfig.getItem(blockBreakData.getBlockData().id());
                                }
                                if (itemStackSupplier == null) {
                                    itemStackSupplier = () -> new ItemStack(logData.worldBlockType());
                                }
                            }
                            if (itemStackSupplier != null) {
                                final World world = block.getWorld();
                                final ItemStack itemStack = itemStackSupplier.get();
                                if (itemStack != null) {
                                    final Supplier<ItemStack> finalItemStackSupplier = itemStackSupplier;
                                    Bukkit.getScheduler().runTaskLater(BlockBreakManager.this.plugin, () -> world.dropItem(block.getLocation().clone().add(0.5, 0, 0.5), finalItemStackSupplier.get()), 1);
                                }
                            }
                            blockBreakData.getBreakTask().cancel();
                            PacketUtils.removeMiningFatigue(player);
                            BlockBreakManager.this.blockBreakDataMap.remove(uuid);
                            return;
                        }
                        final int updatedBlockBreakTime = BlockBreakManager.this.calculateBlockBreakTimeInTicks(player, player.getInventory().getItemInMainHand());
                        final double percentageToAdd = (double) blockBreakTime / updatedBlockBreakTime;
                        blockBreakData.addBreakTimeProgress(percentageToAdd);
//                        blockBreakData.addBreakTimeProgress(period);
                        blockBreakData.send(blockBreakData.getPosition());
                }, 1, 1);
    }

    private static final int LOG_HARDNESS = 2;

    private static final Map<Material, Integer> TOOL_SPEED_MULTIPLIERS = Map.of(
            Material.GOLDEN_AXE, 12,
            Material.NETHERITE_AXE, 9,
            Material.DIAMOND_AXE, 8,
            Material.IRON_AXE, 6,
            Material.STONE_AXE, 4,
            Material.WOODEN_AXE, 2
    );

    // formula found at https://minecraft.fandom.com/wiki/Breaking#Speed
    private int calculateBlockBreakTimeInTicks(Player player, ItemStack inHand) {
        final Material material = inHand.getType();
        double speedMultiplier = TOOL_SPEED_MULTIPLIERS.getOrDefault(material, 1);
        final int efficiencyLevel = inHand.getEnchantmentLevel(Enchantment.DIG_SPEED);
        if (efficiencyLevel != 0) {
            speedMultiplier += Math.pow(efficiencyLevel, 2) + 1;
        }
        final PotionEffect haste = player.getPotionEffect(PotionEffectType.FAST_DIGGING);
        if (haste != null) {
            speedMultiplier *= 0.2 * haste.getAmplifier() + 1;
        }
        final PotionEffect miningFatigue = player.getPotionEffect(PotionEffectType.SLOW_DIGGING);
        if (miningFatigue != null) {
            speedMultiplier *= Math.pow(0.3, Math.min(miningFatigue.getAmplifier(), 4));
        }
        final ItemStack helmet = player.getInventory().getHelmet();
        if (player.isInWater()) {
            if (helmet == null || helmet.getEnchantmentLevel(Enchantment.WATER_WORKER) <= 0) {
                speedMultiplier /= 5;
            }
        }
        if (!player.isOnGround()) {
            speedMultiplier /= 5;
        }
        final double damage = (speedMultiplier / LOG_HARDNESS) / 30;
        if (damage > 1) {
            return 0;
        }
        return (int) Math.ceil(1 / damage);
    }

//    public void setBlockBreakData(UUID uuid, BlockBreakData blockBreakData) {
//        this.blockBreakDataMap.put(uuid, blockBreakData);
//    }

    private static class BlockBreakData {

        public static final byte MAX_DAMAGE = 10;

        private final int entityId;
        private final BlockData blockData;
        private final Player breaker;
        private final Position position;
        private final ItemStack itemInHand;
        private final long startTime;
        private long lastHitTime;
        private final int totalBreakTime;
        private double breakTimeProgress;
        private final BukkitTask breakTask;

        public BlockBreakData(
                int entityId,
                BlockData blockData,
                Player breaker,
                Position position,
                ItemStack itemInHand,
                long startTime,
                long lastHitTime,
                int totalBreakTime,
                int breakTimeProgress,
                BukkitTask breakTask
        ) {
            this.entityId = entityId;
            this.blockData = blockData;
            this.breaker = breaker;
            this.position = position;
            this.itemInHand = itemInHand;
            this.startTime = startTime;
            this.lastHitTime = lastHitTime;
            this.totalBreakTime = totalBreakTime;
            this.breakTimeProgress = breakTimeProgress;
            this.breakTask = breakTask;
        }

        public int getEntityId() {
            return entityId;
        }

        public BlockData getBlockData() {
            return blockData;
        }

        public Player getBreaker() {
            return breaker;
        }

        public Position getPosition() {
            return position;
        }

        public ItemStack getItemInHand() {
            return itemInHand;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getLastHitTime() {
            return lastHitTime;
        }

        public void setLastHitTime(long lastHitTime) {
            this.lastHitTime = lastHitTime;
        }

        public int getTotalBreakTime() {
            return totalBreakTime;
        }

        public double getBreakTimeProgress() {
            return breakTimeProgress;
        }

        public void setBreakTimeProgress(double breakTimeProgress) {
            this.breakTimeProgress = breakTimeProgress;
        }

        public void addBreakTimeProgress(double breakTimeProgress) {
            this.breakTimeProgress = Math.min(this.breakTimeProgress + breakTimeProgress, this.totalBreakTime);
        }

        public byte calculateDamage() {
            final double percentage = (double) this.breakTimeProgress / this.totalBreakTime;
            final double damage = MAX_DAMAGE * percentage;
            return (byte) (Math.min(damage, MAX_DAMAGE) - 1);
        }

        public void send(Position position) {
            final byte calculatedDamage = this.calculateDamage();
            PacketUtils.sendBlockBreakAnimation(this.breaker, position.toLocation(), this.entityId, calculatedDamage);
        }

        public boolean isBroken() {
            return this.breakTimeProgress >= this.totalBreakTime;
        }

        public BukkitTask getBreakTask() {
            return breakTask;
        }

    }

}
