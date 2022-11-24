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

package io.github.fisher2911.hmcleaves.util;

import com.jeff_media.customblockdata.CustomBlockData;
import io.github.fisher2911.hmcleaves.Config;
import io.github.fisher2911.hmcleaves.FakeLeafState;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.LeafCache;
import io.github.fisher2911.hmcleaves.util.collection.UniqueConcurrentLinkedDeque;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class LeafUpdater3 {

    private static final int LEAF_DECAY_DISTANCE = 7;

    private static final BlockFace[] DIRECTIONS = new BlockFace[]{
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
    };


    private static final HMCLeaves PLUGIN = HMCLeaves.getPlugin(HMCLeaves.class);

    private static final AtomicLong CURRENT_TICK = new AtomicLong(0);

    public static long getCurrentTick() {
        return CURRENT_TICK.get();
    }

    private static final Map<Long, ChunkSnapshot> cachedSnapshots = new ConcurrentHashMap<>();
    //    private static final Multimap<Long, Location> leafStatesToTick = Multimaps.newMultimap(
//            new ConcurrentHashMap<>(),
//            ConcurrentLinkedDeque::new
//    );
    private static final Map<Long, UniqueConcurrentLinkedDeque<Location>> leafStatesToTick = new ConcurrentHashMap<>();
    private static final Map<Location, FakeLeafState> toUpdate = new HashMap<>();

    private static BukkitTask task;

    private static final int MAX_UPDATE_DEPTH = 10_000;

    public static void start() {
        if (task != null && !task.isCancelled()) {
            throw new IllegalStateException("Can not run more than one leaf updater at a time!");
        }
        final AtomicBoolean previousTaskFinished = new AtomicBoolean(true);
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(PLUGIN, () -> {
            if (!previousTaskFinished.get()) return;
            previousTaskFinished.set(false);
            if (leafStatesToTick.isEmpty()) {
                CURRENT_TICK.addAndGet(1);
                previousTaskFinished.set(true);
                return;
            }
            final long currentTick = CURRENT_TICK.get();
            final UniqueConcurrentLinkedDeque<Location> toTick = removeToTick(currentTick);
            if (toTick.isEmpty()) {
                CURRENT_TICK.addAndGet(1);
                previousTaskFinished.set(true);
                return;
            }
            cachedSnapshots.clear();
            final UniqueConcurrentLinkedDeque<Location> nextToTick = getToTick(currentTick + 1);
            int updateDepth = 0;
            for (Location location : toTick) {
                if (updateDepth > MAX_UPDATE_DEPTH) {
                    nextToTick.add(location);
                    continue;
                }
                if (tick(location)) {
                    updateDepth++;
                }
            }
            if (nextToTick.isEmpty()) removeToTick(currentTick + 1);
//            Bukkit.broadcastMessage("Reached update depth: " + updateDepth);
//            Bukkit.broadcastMessage("To update size: " + toUpdate.size());
            final Map<Location, FakeLeafState> toUpdateCopy = new HashMap<>(toUpdate);
            toUpdate.clear();
            final LeafCache leafCache = PLUGIN.getLeafCache();
            final Config config = PLUGIN.config();
            CURRENT_TICK.addAndGet(1);
            previousTaskFinished.set(true);
            Bukkit.getScheduler().runTask(PLUGIN, () -> {
                for (var entry : toUpdateCopy.entrySet()) {
                    final Location location = entry.getKey();
                    final BlockData blockData = location.getBlock().getBlockData();
                    final FakeLeafState state = entry.getValue();
                    final Block block = location.getBlock();
                    if (state == null) {
                        if (config.isLogBlock(blockData)) {
                            PDCUtil.setLogBlock(new CustomBlockData(block, PLUGIN));
                            leafCache.setLogAt(location);
                            continue;
                        }
                        if (leafCache.isLogAt(location)) {
                            leafCache.removeLogAt(location);
                            PDCUtil.removeTreeBlock(new CustomBlockData(block, PLUGIN));
                            continue;
                        }
                        continue;
                    }
                    if (!(blockData instanceof final Leaves leaves)) {
                        leafCache.remove(location);
                        PDCUtil.clearLeafData(new CustomBlockData(block, PLUGIN));
                        continue;
                    }
                    final CustomBlockData customBlockData = new CustomBlockData(block, PLUGIN);
                    PDCUtil.setActualDistance(customBlockData, (byte) state.actualDistance());
                    PDCUtil.setActualPersistent(customBlockData, state.actuallyPersistent());
                    PDCUtil.setDistance(customBlockData, (byte) state.state().getDistance());
                    PDCUtil.setPersistent(customBlockData, (byte) (state.state().isPersistent() ? 1 : 0));
                    if (
                            (state.actuallyPersistent() || state.actualDistance() < LEAF_DECAY_DISTANCE) &&
                                    (leaves.getDistance() >= LEAF_DECAY_DISTANCE)
                    ) {
                        if (!leaves.isPersistent()) {
                            leaves.setPersistent(true);
                            block.setBlockData(leaves, false);
                        }
                        continue;
                    }
                    leaves.setPersistent(false);
                    block.setBlockData(leaves, false);
                }
            });
        }, 1, 1);

    }

    private static UniqueConcurrentLinkedDeque<Location> getToTick(long tickTime) {
        return leafStatesToTick.computeIfAbsent(tickTime, k -> new UniqueConcurrentLinkedDeque<>());
    }

    private static UniqueConcurrentLinkedDeque<Location> removeToTick(long tickTime) {
        final UniqueConcurrentLinkedDeque<Location> queue = leafStatesToTick.remove(tickTime);
        if (queue == null) {
            return new UniqueConcurrentLinkedDeque<>();
        }
        return queue;
    }

    private static void addToTick(Location location) {
        getToTick(CURRENT_TICK.get() + 1L).add(location);
    }

    /**
     * @param location
     * @return true if the block needs to be updated
     */
    private static boolean tick(Location location) {
        final LeafCache leafCache = PLUGIN.getLeafCache();
        final Config config = PLUGIN.config();
        final BlockData blockData = getBlockData(location);
        final FakeLeafState state = leafCache.getAt(location);
        final boolean isLog = state == null && leafCache.isLogAt(location);
        final boolean logAdded = !isLog && config.isLogBlock(blockData);
        final boolean logRemoved = isLog && !config.isLogBlock(blockData);

        if (logAdded || logRemoved) {
            toUpdate.put(location, null);
            updateNeighbors(leafCache, location);
            return true;
        }
//        if (state == null) return false;
        final int distance = getLowestDistance(leafCache, location);
        if (state != null && distance == state.actualDistance() && blockData instanceof final Leaves leaves) {
            if (
                    !leaves.isPersistent() &&
                            (state.actuallyPersistent() || state.actualDistance() < LEAF_DECAY_DISTANCE) &&
                            leaves.getDistance() >= LEAF_DECAY_DISTANCE
            ) {
                return toUpdate.put(location, state) == null;
            }
            if (leaves.isPersistent() &&
                    !state.actuallyPersistent() &&
                    state.actualDistance() >= LEAF_DECAY_DISTANCE
            ) {
                return toUpdate.put(location, state) == null;
            }
            return false;
        }
        if (state != null && blockData instanceof Leaves) {
            state.actualDistance(distance);
        } else if (state != null) {
            state.actualDistance(7);
        }
        final boolean added = toUpdate.put(location, state) == null;
        updateNeighbors(leafCache, location);
        return added;
    }

    private static void updateNeighbors(LeafCache leafCache, Location location) {
        for (BlockFace direction : DIRECTIONS) {
            final Location relative = location.clone().add(direction.getDirection());
            final FakeLeafState state = leafCache.getAt(relative);
            if (state == null) continue;
            scheduleTick(relative);
        }
    }

    public static void scheduleTick(Location location) {
        addToTick(location);
    }

    private static int getLowestDistance(LeafCache leafCache, Location location) {
        int distance = LEAF_DECAY_DISTANCE;
        for (BlockFace face : DIRECTIONS) {
            final Location relativeLocation = location.clone().add(face.getDirection());
            final BlockData relative = getBlockData(relativeLocation);
            FakeLeafState relativeState = leafCache.getAt(relativeLocation);
            if (relativeState == null && relative instanceof Leaves) {
                relativeState = leafCache.createAtOrGetAndSet(relativeLocation, relative.getMaterial());
            }
            distance = Math.min(distance, getDistanceAt(leafCache, relativeLocation, relativeState) + 1);
            if (distance == 1) break;
        }
        return distance;
    }

    private static int getDistanceAt(LeafCache cache, Location location, @Nullable FakeLeafState state) {
        if (cache.isLogAt(location)) return 0;
        return state == null ? LEAF_DECAY_DISTANCE : state.actualDistance();
    }

    @Nullable
    private static ChunkSnapshot getOrCacheSnapshot(Location location) {
        final int chunkX = location.getBlockX() >> 4;
        final int chunkZ = location.getBlockZ() >> 4;
        final World world = location.getWorld();
        if (world == null || !world.isChunkLoaded(chunkX, chunkZ)) return null;
        final long chunkKey = ChunkUtil.chunkKeyAt(chunkX, chunkZ);
        return cachedSnapshots.computeIfAbsent(chunkKey, key -> location.getChunk().getChunkSnapshot());
    }

    @Nullable
    private static BlockData getBlockData(Location location) {
        final ChunkSnapshot snapshot = getOrCacheSnapshot(location);
        if (snapshot == null) return null;
        final int x = ChunkUtil.getCoordInChunk(location.getBlockX());
        final int y = location.getBlockY();
        final int z = ChunkUtil.getCoordInChunk(location.getBlockZ());
        return snapshot.getBlockData(x, y, z);
    }

}
