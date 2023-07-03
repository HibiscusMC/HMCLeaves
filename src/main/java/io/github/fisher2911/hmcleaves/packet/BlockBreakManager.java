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
import io.github.fisher2911.hmcleaves.data.MineableData;
import io.github.fisher2911.hmcleaves.util.ItemUtil;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

    private static final int LOG_HARDNESS = 2;
    public static final BlockBreakModifier LOG_BREAK_MODIFIER = new BlockBreakModifier(LOG_HARDNESS, false, Set.of(BlockBreakModifier.ToolType.AXE));

    public <T extends MineableData & BlockData> void startBlockBreak(Player player, Position position, T mineableData) {
        if (mineableData.blockBreakModifier() == null) return;
        final int blockBreakTime = this.calculateBlockBreakTimeInTicks(
                player,
                player.getInventory().getItemInMainHand(),
                mineableData.blockBreakModifier()
        );
//        final int period = (int) Math.ceil(blockBreakTime / (double) BlockBreakData.MAX_DAMAGE);
        final BlockBreakData blockBreakData = new BlockBreakData(
                RANDOM.nextInt(10_000, 20_000),
                mineableData,
                player,
                position,
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
                        final LeavesBlockBreakEvent event = new LeavesBlockBreakEvent(block, player);
                        Bukkit.getPluginManager().callEvent(event);
                        if (event.isCancelled()) {
                            blockBreakData.resetProgress();
                            return;
                        }
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
                        final BlockBreakModifier blockBreakModifier = blockBreakData.getBlockData().blockBreakModifier();
                        final ItemStack heldItem = blockBreakData.getBreaker().getInventory().getItemInMainHand();
                        if (!blockBreakModifier.requiresToolToDrop() || blockBreakModifier.hasToolType(heldItem.getType())) {
                            if (blockBreakData.getBlockData() instanceof final LogData logData) {
                                itemStackSupplier = BlockBreakManager.this.leavesConfig.getItemSupplier(logData.getCurrentId());
                            } else {
                                itemStackSupplier = BlockBreakManager.this.leavesConfig.getItemSupplier(blockBreakData.getBlockData().id());
                            }
                            if (itemStackSupplier == null) {
                                itemStackSupplier = () -> new ItemStack(blockBreakData.getBlockData().worldBlockType());
                            }
                        }
                        if (event.isDropItems() && itemStackSupplier != null) {
                            final World world = block.getWorld();
                            final ItemStack itemStack = itemStackSupplier.get();
                            if (itemStack != null) {
                                final Supplier<ItemStack> finalItemStackSupplier = itemStackSupplier;
                                Bukkit.getScheduler().runTaskLater(BlockBreakManager.this.plugin, () -> world.dropItem(block.getLocation().clone().add(0.5, 0, 0.5), finalItemStackSupplier.get()), 1);
                            }
                        }
                        blockBreakData.getBreakTask().cancel();
                        if (!ItemUtil.isQuickMiningTool(heldItem.getType())) {
                            PacketUtils.removeMiningFatigue(player);
                        }
                        BlockBreakManager.this.blockBreakDataMap.remove(uuid);
                        return;
                    }
                    final int updatedBlockBreakTime = BlockBreakManager.this.calculateBlockBreakTimeInTicks(
                            player,
                            player.getInventory().getItemInMainHand(),
                            blockBreakData.getBlockData().blockBreakModifier()
                    );
                    final double percentageToAdd = (double) blockBreakTime / updatedBlockBreakTime;
                    blockBreakData.addBreakTimeProgress(percentageToAdd);
//                        blockBreakData.addBreakTimeProgress(period);
                    blockBreakData.send(blockBreakData.getPosition());
                }, 1, 1);
    }

    private static final Map<Material, Integer> TOOL_SPEED_MULTIPLIERS = createToolSpeedModifiers();

    // formula found at https://minecraft.fandom.com/wiki/Breaking#Speed
    private int calculateBlockBreakTimeInTicks(Player player, ItemStack inHand, BlockBreakModifier modifier) {
        final Material material = inHand.getType();
        double speedMultiplier = TOOL_SPEED_MULTIPLIERS.getOrDefault(material, 1);
        if (!modifier.hasToolType(material)) {
            speedMultiplier = 1;
        }
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
        final double damage = (speedMultiplier / modifier.hardness()) / 30;
        if (damage > 1) {
            return 0;
        }
        return (int) Math.ceil(1 / damage);
    }

    private static Map<Material, Integer> createToolSpeedModifiers() {
        final Map<Material, Integer> map = new HashMap<>();
        final Set<Material> goldTools = Set.of(Material.GOLDEN_SHOVEL, Material.GOLDEN_PICKAXE, Material.GOLDEN_AXE, Material.GOLDEN_HOE, Material.GOLDEN_SWORD);
        final Set<Material> netheriteTools = Set.of(Material.NETHERITE_SHOVEL, Material.NETHERITE_PICKAXE, Material.NETHERITE_AXE, Material.NETHERITE_HOE, Material.NETHERITE_SWORD);
        final Set<Material> diamondTools = Set.of(Material.DIAMOND_SHOVEL, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_HOE, Material.DIAMOND_SWORD);
        final Set<Material> ironTools = Set.of(Material.IRON_SHOVEL, Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_HOE, Material.IRON_SWORD);
        final Set<Material> stoneTools = Set.of(Material.STONE_SHOVEL, Material.STONE_PICKAXE, Material.STONE_AXE, Material.STONE_HOE, Material.STONE_SWORD);
        final Set<Material> woodenTools = Set.of(Material.WOODEN_SHOVEL, Material.WOODEN_PICKAXE, Material.WOODEN_AXE, Material.WOODEN_HOE, Material.WOODEN_SWORD);
        goldTools.forEach(material -> map.put(material, 12));
        netheriteTools.forEach(material -> map.put(material, 9));
        diamondTools.forEach(material -> map.put(material, 8));
        ironTools.forEach(material -> map.put(material, 6));
        stoneTools.forEach(material -> map.put(material, 4));
        woodenTools.forEach(material -> map.put(material, 2));
        map.put(Material.SHEARS, 2);
        return map;
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
        private final int totalBreakTime;
        private double breakTimeProgress;
        private final BukkitTask breakTask;

        public <T extends BlockData & MineableData> BlockBreakData(
                int entityId,
                T blockData,
                Player breaker,
                Position position,
                int totalBreakTime,
                int breakTimeProgress,
                BukkitTask breakTask
        ) {
            this.entityId = entityId;
            this.blockData = blockData;
            this.breaker = breaker;
            this.position = position;
            this.totalBreakTime = totalBreakTime;
            this.breakTimeProgress = breakTimeProgress;
            this.breakTask = breakTask;
        }

        public int getEntityId() {
            return entityId;
        }

        public <T extends BlockData & MineableData> T getBlockData() {
            return (T) this.blockData;
        }

        public Player getBreaker() {
            return breaker;
        }

        public Position getPosition() {
            return position;
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

        public void resetProgress() {
            this.breakTimeProgress = 0;
        }

    }

    public static class LeavesBlockBreakEvent extends BlockBreakEvent {

        public LeavesBlockBreakEvent(@NotNull Block theBlock, @NotNull Player player) {
            super(theBlock, player);
        }

    }

}
