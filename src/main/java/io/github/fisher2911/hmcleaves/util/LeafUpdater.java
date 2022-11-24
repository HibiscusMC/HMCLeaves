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

public class LeafUpdater {

//    private static final HMCLeaves PLUGIN = HMCLeaves.getPlugin(HMCLeaves.class);
//    private static BukkitTask leafCheckTask;
//    private static final Queue<LeafUpdater> queue = new ConcurrentLinkedQueue<>();
//    private static final int MAX_TASK_COUNT = 100;
//    private static int currentTaskCount = 0;
//
//    private static final AtomicInteger taskCount = new AtomicInteger(0);
//
//    private static void checkDoTask() {
//        if (queue.isEmpty()) return;
//        if (leafCheckTask == null || leafCheckTask.isCancelled()) {
//            final int taskNumber = taskCount.getAndIncrement();
//            leafCheckTask = Bukkit.getScheduler().runTaskTimerAsynchronously(PLUGIN, () -> {
//                LeafUpdater updater = queue.poll();
//                while (updater != null && currentTaskCount < MAX_TASK_COUNT) {
//                    updater.update();
//                    currentTaskCount++;
//                    updater = queue.poll();
//                }
////            Bukkit.broadcastMessage("Task count size reached: " + currentTaskCount);
////                Bukkit.broadcastMessage("Task running: " + taskNumber + " count: " + currentTaskCount);
//                currentTaskCount = 0;
//                if (queue.isEmpty()) {
//                    leafCheckTask.cancel();
//                }
//            }, 0, 1);
//        }
//    }
//
//    private static final BlockFace[] DIRECTIONS = new BlockFace[]{
//            BlockFace.NORTH,
//            BlockFace.EAST,
//            BlockFace.SOUTH,
//            BlockFace.WEST,
//            BlockFace.UP,
//            BlockFace.DOWN
//    };
//
//    private final LeafCache leafCache;
//    private final Map<Location, BlockData> blocksToUpdate;
//    private final Map<Location, FakeLeafState> originalLeavesData;
//    private final Set<Location> addedLogs;
//    private final Set<Location> removedLogs;
//    private final Map<Long, ChunkSnapshot> cachedSnapshots;
//    private final Set<Location> checked;
//
//    private LeafUpdater(Map<Location, BlockData> blocksToUpdate, Set<Location> addedLogs, Set<Location> removedLogs) {
//        this.leafCache = PLUGIN.getLeafCache();
//        this.blocksToUpdate = blocksToUpdate;
//        this.cachedSnapshots = new HashMap<>();
//        this.originalLeavesData = new HashMap<>();
//        this.checked = new HashSet<>();
//        this.addedLogs = addedLogs;
//        this.removedLogs = removedLogs;
//    }
//
//    public static LeafUpdater doUpdate(
//            Map<Location, BlockData> blocksToUpdate,
//            Set<Location> addedLogs,
//            Set<Location> removedLogs
//    ) {
//        final LeafUpdater updater = new LeafUpdater(blocksToUpdate, addedLogs, removedLogs);
//        queue.add(updater);
//        checkDoTask();
////        updater.update();
//        return updater;
//    }
//
//    public static LeafUpdater doUpdate(
//            Map<Location, BlockData> blocksToUpdate
//    ) {
//        return doUpdate(blocksToUpdate, new HashSet<>(), new HashSet<>());
//    }
//
//    private int count = 0;
//
//    private void update() {
//        for (var entry : this.blocksToUpdate.entrySet()) {
//            final Location location = entry.getKey();
//            if (this.checked.contains(location)) continue;
//            final BlockData blockData = entry.getValue();
//            FakeLeafState state = this.leafCache.getAt(location);
////            if (/*state == null && */(!this.leafCache.isLogAt(location) && !this.addedLogs.contains(location) && !this.removedLogs.contains(location))) {
////                if (!(blockData instanceof Leaves leaves)) continue;
////                final FakeLeafState fakeLeafState = this.leafCache.createAtOrGetAndSet(location, blockData.getMaterial());
////                fakeLeafState.actualDistance(leaves.getDistance());
////                fakeLeafState.actuallyPersistent(leaves.isPersistent());
////            }
//            this.updateDistances(location, state);
//        }
//        this.originalLeavesData.entrySet().removeIf(entry -> {
//            final Location location = entry.getKey();
//            if (this.addedLogs.contains(location) || this.removedLogs.contains(location)) return true;
//            final FakeLeafState state = entry.getValue();
//            final FakeLeafState newState = this.leafCache.getAt(location);
//            if (newState == null) return false;
//            return newState.actualDistance() == state.actualDistance() && newState.actuallyPersistent() == state.actuallyPersistent();
//        });
////        Bukkit.broadcastMessage("Original size: " + this.blocksToUpdate.size());
////        Bukkit.broadcastMessage("Checked " + this.checked.size() + " blocks and total changed were " + this.originalLeavesData.size());
//        Bukkit.getScheduler().runTask(PLUGIN, this::updateChanged);
//    }
//
//    private void updateChanged() {
//        for (var entry : this.originalLeavesData.entrySet()) {
//            final Location location = entry.getKey();
//            final FakeLeafState currentState = this.leafCache.getAt(location);
//            if (currentState == null) {
//                continue;
//            }
//            final CustomBlockData customBlockData = new CustomBlockData(location.getBlock(), PLUGIN);
//            PDCUtil.setActualPersistent(customBlockData, currentState.actuallyPersistent());
//            PDCUtil.setActualDistance(customBlockData, (byte) currentState.actualDistance());
//            if (currentState.actualDistance() < 7 || currentState.actuallyPersistent()) continue;
//            final Block block = location.getBlock();
//            if (!(block.getBlockData() instanceof Leaves leaves)) continue;
//            leaves.setDistance(7);
//            leaves.setPersistent(false);
//            block.setBlockData(leaves, false);
//
//        }
//        for (Location log : this.addedLogs) {
//            final CustomBlockData customBlockData = new CustomBlockData(log.getBlock(), PLUGIN);
//            PDCUtil.setLogBlock(customBlockData);
//        }
//        for (Location log : this.removedLogs) {
//            final CustomBlockData customBlockData = new CustomBlockData(log.getBlock(), PLUGIN);
//            customBlockData.remove(PDCUtil.LOG_BLOCK_KEY);
//        }
////        Bukkit.broadcastMessage("Total checked: " + count);
//    }
//
//    private void updateDistances(Location location, @Nullable FakeLeafState state) {
//        this.count++;
//        this.checked.add(location);
//        if (state != null) {
//            this.originalLeavesData.putIfAbsent(location, state.snapshot());
//        }
//        final boolean isLog = this.leafCache.isLogAt(location) || this.addedLogs.contains(location) || this.removedLogs.contains(location);
////        Bukkit.broadcastMessage("Checking " + location + " - is log: " + isLog);
//        if (!isLog && state == null && !this.blocksToUpdate.containsKey(location)) return;
//        final int distance = getLowestDistance(location);
//        if (!isLog && state != null && distance == state.actualDistance()) return;
//        if (!isLog && state != null) state.actualDistance(distance);
//        for (BlockFace face : DIRECTIONS) {
//            final Location relativeLocation = location.clone().add(face.getDirection());
//            final BlockData relative = this.getBlockData(relativeLocation);
//            FakeLeafState relativeState = this.leafCache.getAt(relativeLocation);
//            if (relativeState == null && relative instanceof Leaves) {
//                relativeState = this.leafCache.createAtOrGetAndSet(relativeLocation, relative.getMaterial());
//            }
//            if (relativeState == null) continue;
//            updateDistances(relativeLocation, relativeState);
//        }
//    }
//
//    private int getLowestDistance(Location location) {
//        int distance = 7;
//        for (BlockFace face : DIRECTIONS) {
//            final Location relativeLocation = location.clone().add(face.getDirection());
//            final BlockData relative = this.getBlockData(relativeLocation);
//            FakeLeafState relativeState = this.leafCache.getAt(relativeLocation);
//            if (relativeState == null && relative instanceof Leaves) {
//                relativeState = this.leafCache.createAtOrGetAndSet(relativeLocation, relative.getMaterial());
//            }
//            distance = Math.min(distance, getDistanceAt(this.leafCache, relativeLocation, relativeState) + 1);
//            if (distance == 1) break;
//        }
//        return distance;
//    }
//
//    private int getDistanceAt(LeafCache cache, Location location, @Nullable FakeLeafState state) {
//        if (cache.isLogAt(location)) return 0;
//        return state == null ? 7 : state.actualDistance();
//    }
//
//    @Nullable
//    private ChunkSnapshot getOrCacheSnapshot(Location location) {
//        final int chunkX = location.getBlockX() >> 4;
//        final int chunkZ = location.getBlockZ() >> 4;
//        final World world = location.getWorld();
//        if (world == null || !world.isChunkLoaded(chunkX, chunkZ)) return null;
//        final long chunkKey = ChunkUtil.chunkKeyAt(chunkX, chunkZ);
//        return this.cachedSnapshots.computeIfAbsent(chunkKey, key -> location.getChunk().getChunkSnapshot());
//    }
//
//    @Nullable
//    private BlockData getBlockData(Location location) {
//        final ChunkSnapshot snapshot = this.getOrCacheSnapshot(location);
//        if (snapshot == null) return null;
//        final int x = ChunkUtil.getCoordInChunk(location.getBlockX());
//        final int y = location.getBlockY();
//        final int z = ChunkUtil.getCoordInChunk(location.getBlockZ());
//        return snapshot.getBlockData(x, y, z);
//    }

}
