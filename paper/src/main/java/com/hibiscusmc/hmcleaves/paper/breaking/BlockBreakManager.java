package com.hibiscusmc.hmcleaves.paper.breaking;

import com.hibiscusmc.hmcleaves.paper.api.HMCLeavesBreakEvent;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlock;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlockState;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorldManager;
import com.hibiscusmc.hmcleaves.paper.world.Position;
import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.config.BlockDropConfig;
import com.hibiscusmc.hmcleaves.paper.config.LeavesConfig;
import com.hibiscusmc.hmcleaves.paper.packet.PacketUtil;
import com.hibiscusmc.hmcleaves.paper.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockBreakManager {

    private static final SplittableRandom RANDOM = new SplittableRandom();

    private final HMCLeaves plugin;
    private final LeavesConfig leavesConfig;
    private final LeavesWorldManager worldManager;
    private final Map<UUID, BlockBreakData> blockBreakDataMap;


    public BlockBreakManager(HMCLeaves plugin) {
        this.blockBreakDataMap = new ConcurrentHashMap<>();
        this.plugin = plugin;
        this.leavesConfig = this.plugin.leavesConfig();
        this.worldManager = plugin.worldManager();
    }

    @Nullable
    private BlockBreakData getBlockBreakData(UUID uuid) {
        return this.blockBreakDataMap.get(uuid);
    }

    public void startBlockBreak(Player player, Position position, CustomBlockState customBlockState) {
        if (!this.leavesConfig.handleMining()) {
            return;
        }
        final CustomBlock customBlock = customBlockState.customBlock();
        if (!Tag.LOGS.isTagged(customBlock.worldMaterial())) {
            return;
        }
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            final BlockBreakData blockBreakData = new BlockBreakData(
                    RANDOM.nextInt(10_000, 20_000),
                    customBlockState,
                    player,
                    position,
                    this.createScheduler(player.getUniqueId())
            );
            this.blockBreakDataMap.put(player.getUniqueId(), blockBreakData);
        });
    }

    public void cancelBlockBreak(Player player) {
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            final BlockBreakData blockBreakData = this.blockBreakDataMap.get(player.getUniqueId());
            if (blockBreakData == null) {
                return;
            }
            PacketUtil.sendBlockBreakAnimation(player, blockBreakData.getLocation(), blockBreakData.entityId(), (byte) -1);
            blockBreakData.breakTask().cancel();
            this.blockBreakDataMap.remove(player.getUniqueId());
        });
    }

    private BukkitTask createScheduler(UUID uuid) {
        return Bukkit.getScheduler().runTaskTimer(this.plugin,
                () -> {
                    final BlockBreakData blockBreakData = BlockBreakManager.this.blockBreakDataMap.get(uuid);
                    if (blockBreakData == null) {
                        return;
                    }
                    final Player player = blockBreakData.breaker();
                    blockBreakData.incrementTick();
                    final byte damage = BlockBreakManager.this.calculateBlockBreakTimeInTicks(
                            player,
                            player.getInventory().getItemInMainHand(),
                            blockBreakData.getLocation().getBlock(),
                            blockBreakData.currentTick()
                    );
                    if (blockBreakData.isBroken(damage)) {
                        this.breakBlock(blockBreakData, player, uuid);
                        return;
                    }
                    blockBreakData.send(damage);
                }, 1, 1);
    }

    public void finishDigging(Player player) {
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            final UUID uuid = player.getUniqueId();
            final BlockBreakData blockBreakData = this.blockBreakDataMap.get(uuid);
            this.breakBlock(blockBreakData, player, uuid);
        });
    }

    private void breakBlock(BlockBreakData blockBreakData, Player player, UUID uuid) {
        blockBreakData.breakTask.cancel();
        final Location location = blockBreakData.getLocation();
        final Block block = location.getBlock();
        final HMCLeavesBreakEvent event = new HMCLeavesBreakEvent(block, player);
        if (!event.callEvent()) {
            BlockBreakManager.this.blockBreakDataMap.remove(uuid);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            PacketUtil.sendBlockBreakAnimation(
                    blockBreakData.breaker(),
                    location,
                    blockBreakData.entityId(),
                    (byte) -1
            );
            PacketUtil.sendBlockBroken(
                    player,
                    blockBreakData.position(),
                    blockBreakData.customBlockState().globalStateId()
            );
        });
        final World world = blockBreakData.world();
        BlockBreakManager.this.blockBreakDataMap.remove(uuid);
        BlockBreakManager.this.worldManager.removeBlock(world.getUID(), blockBreakData.position());
        PacketUtil.removeMiningFatigue(player);
        block.setType(Material.AIR);
        final CustomBlock customBlock = blockBreakData.customBlockState().customBlock();
        final BlockDropConfig drops = this.leavesConfig.getBlockDrops(customBlock.id());
        if (drops != null) {
            final ItemStack drop = drops.copyLeavesItem();
            if (drop != null) {
                world.dropItemNaturally(location, drop);
            }
        }
    }

    private byte calculateBlockBreakTimeInTicks(Player player, ItemStack inHand, Block block, int ticksPassed) {
        final float destroySpeed = block.getDestroySpeed(inHand, true);
        final float hardness = block.getType().getHardness();
        final boolean isCorrectTool = !block.getDrops(inHand).isEmpty();
        return (byte) ((destroySpeed / hardness / (isCorrectTool ? 30 : 100)) * ticksPassed * 10);
    }

    private static class BlockBreakData {

        public static final byte MAX_DAMAGE = 10;

        private final int entityId;
        private final CustomBlockState customBlockState;
        private final Player breaker;
        private final World world;
        private final Position position;
        private final BukkitTask breakTask;
        private int currentTick;

        public BlockBreakData(
                int entityId,
                CustomBlockState customBlockState,
                Player breaker,
                Position position,
                BukkitTask breakTask
        ) {
            this.entityId = entityId;
            this.customBlockState = customBlockState;
            this.breaker = breaker;
            this.world = this.breaker.getWorld();
            this.position = position;
            this.breakTask = breakTask;
        }

        public int entityId() {
            return this.entityId;
        }

        public CustomBlockState customBlockState() {
            return this.customBlockState;
        }

        public Player breaker() {
            return this.breaker;
        }

        public World world() {
            return this.world;
        }

        public Position position() {
            return this.position;
        }

        public Location getLocation() {
            return WorldUtil.convertPosition(this.world, this.position);
        }

        public int currentTick() {
            return this.currentTick;
        }

        public void incrementTick() {
            this.currentTick++;
        }

        public void send(byte progress) {
            PacketUtil.sendBlockBreakAnimation(this.breaker, this.getLocation(), this.entityId, progress);
        }

        public boolean isBroken(byte progress) {
            return progress >= MAX_DAMAGE;
        }

        public BukkitTask breakTask() {
            return this.breakTask;
        }

        public void resetProgress() {
            this.currentTick = 0;
        }

    }

}