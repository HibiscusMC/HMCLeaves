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

public class LeafUpdater2 {

//    private static final HMCLeaves PLUGIN = HMCLeaves.getPlugin(HMCLeaves.class);
//    private static BukkitTask leafCheckTask;
////    private static final Queue<LeafUpdater2> queue = new ConcurrentLinkedQueue<>();
////    private static final int MAX_TASK_COUNT = 100;
////    private static int currentTaskCount = 0;
//
//    private static final AtomicInteger taskCount = new AtomicInteger(0);
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
//    private static final ConcurrentLinkedDeque<Pair<Location, BlockData>> blocksToUpdate = new ConcurrentLinkedDeque<>();
//    private static final Map<Long, ChunkSnapshot> cachedSnapshots = new ConcurrentHashMap<>();
//    private static final Map<Location, FakeLeafState> toSetState = new ConcurrentHashMap<>();
//
//    private static void checkDoTask() {
//        if (blocksToUpdate.isEmpty()) return;
//        if (leafCheckTask == null || leafCheckTask.isCancelled()) {
//            final int taskNumber = taskCount.getAndIncrement();
//            leafCheckTask = Bukkit.getScheduler().runTaskTimerAsynchronously(PLUGIN, () -> {
////                LeafUpdater2 updater = queue.poll();
////                while (updater != null && currentTaskCount < MAX_TASK_COUNT) {
////                    updater.update();
////                    currentTaskCount++;
////                    updater = queue.poll();
////                }
//                update();
////            Bukkit.broadcastMessage("Task count size reached: " + currentTaskCount);
////                Bukkit.broadcastMessage("Task running: " + taskNumber + " count: " + currentTaskCount);
////                currentTaskCount = 0;
////                if (queue.isEmpty()) {
////                    leafCheckTask.cancel();
////                }
//                if (blocksToUpdate.isEmpty()) {
//                    leafCheckTask.cancel();
//                }
//            }, 0, 1);
//        }
//    }
//
//    private static int count = 0;
//
//    private static final int MAX_BLOCKS_PER_TICK = 100;
//    private static int currentBlockCount;
//
//    public static void addBlocks(final Map<Location, BlockData> blocks) {
//        addBlocks(blocks, Collections.emptySet(), Collections.emptySet());
//    }
//
//    public static void addBlocks(final Map<Location, BlockData> blocks, Set<Location> placedLogs, Set<Location> removedLogs) {
//        Bukkit.getScheduler().runTask(PLUGIN, () -> {
//            for (Location location : placedLogs) {
////                final CustomBlockData customBlockData = new CustomBlockData(location.getBlock(), PLUGIN);
////                PDCUtil.setLogBlock(customBlockData);
//            }
//            for (Location location : removedLogs) {
////                final CustomBlockData customBlockData = new CustomBlockData(location.getBlock(), PLUGIN);
////                PDCUtil.clearLogData(customBlockData);
//            }
//            for (final Map.Entry<Location, BlockData> entry : blocks.entrySet()) {
//                blocksToUpdate.add(new Pair<>(entry.getKey(), entry.getValue()));
//            }
//            checkDoTask();
//        });
//    }
//
//    private static void update() {
//        final LeafCache leafCache = PLUGIN.getLeafCache();
//        while (currentBlockCount < MAX_BLOCKS_PER_TICK && !blocksToUpdate.isEmpty()) {
//            final Pair<Location, BlockData> pair = blocksToUpdate.poll();
//            final Location location = pair.first();
//            final FakeLeafState state = leafCache.getAt(location);
//            updateDistances(leafCache, location, state);
//        }
//        currentBlockCount = 0;
//        final Map<Location, FakeLeafState> copy = new HashMap<>(toSetState);
//        Bukkit.getScheduler().runTask(PLUGIN, () -> {
//            for (var entry : copy.entrySet()) {
//                final Location location = entry.getKey();
////                final CustomBlockData customBlockData = new CustomBlockData(location.getBlock(), PLUGIN);
//                if (leafCache.isLogAt(location)) {
////                    PDCUtil.setLogBlock(customBlockData);
//                    continue;
//                }
//                final FakeLeafState state = entry.getValue();
//                if (state == null) {
////                    PDCUtil.clearLeafData(customBlockData);
//                    return;
//                }
////                PDCUtil.setActualPersistent(customBlockData, state.actuallyPersistent());
////                PDCUtil.setActualDistance(customBlockData, (byte) state.actualDistance());
//                if (state.actualDistance() < 7 || state.actuallyPersistent()) continue;
//                final Block block = location.getBlock();
//                if (!(block.getBlockData() instanceof Leaves leaves)) continue;
//                leaves.setDistance(7);
//                leaves.setPersistent(false);
//                block.setBlockData(leaves, false);
//            }
//        });
////        for (var entry : blocksToUpdate.entrySet()) {
////            final BlockData blockData = entry.getValue();
////            FakeLeafState state = this.leafCache.getAt(location);
////            if (/*state == null && */(!this.leafCache.isLogAt(location) && !this.addedLogs.contains(location) && !this.removedLogs.contains(location))) {
////                if (!(blockData instanceof Leaves leaves)) continue;
////                final FakeLeafState fakeLeafState = this.leafCache.createAtOrGetAndSet(location, blockData.getMaterial());
////                fakeLeafState.actualDistance(leaves.getDistance());
////                fakeLeafState.actuallyPersistent(leaves.isPersistent());
////            }
////            updateDistances(location, state);
////        }
////        this.originalLeavesData.entrySet().removeIf(entry -> {
////            final Location location = entry.getKey();
////            if (this.addedLogs.contains(location) || this.removedLogs.contains(location)) return true;
////            final FakeLeafState state = entry.getValue();
////            final FakeLeafState newState = this.leafCache.getAt(location);
////            if (newState == null) return false;
////            return newState.actualDistance() == state.actualDistance() && newState.actuallyPersistent() == state.actuallyPersistent();
////        });
//////        Bukkit.broadcastMessage("Original size: " + this.blocksToUpdate.size());
//////        Bukkit.broadcastMessage("Checked " + this.checked.size() + " blocks and total changed were " + this.originalLeavesData.size());
////        Bukkit.getScheduler().runTask(PLUGIN, this::updateChanged);
//    }
//
////    private void updateChanged() {
////        for (var entry : this.originalLeavesData.entrySet()) {
////            final Location location = entry.getKey();
////            final FakeLeafState currentState = this.leafCache.getAt(location);
////            if (currentState == null) {
////                continue;
////            }
////            final CustomBlockData customBlockData = new CustomBlockData(location.getBlock(), PLUGIN);
////            PDCUtil.setActualPersistent(customBlockData, currentState.actuallyPersistent());
////            PDCUtil.setActualDistance(customBlockData, (byte) currentState.actualDistance());
////            if (currentState.actualDistance() < 7 || currentState.actuallyPersistent()) continue;
////            final Block block = location.getBlock();
////            if (!(block.getBlockData() instanceof Leaves leaves)) continue;
////            leaves.setDistance(7);
////            leaves.setPersistent(false);
////            block.setBlockData(leaves, false);
////
////        }
////        for (Location log : this.addedLogs) {
////            final CustomBlockData customBlockData = new CustomBlockData(log.getBlock(), PLUGIN);
////            PDCUtil.setLogBlock(customBlockData);
////        }
////        for (Location log : this.removedLogs) {
////            final CustomBlockData customBlockData = new CustomBlockData(log.getBlock(), PLUGIN);
////            customBlockData.remove(PDCUtil.LOG_BLOCK_KEY);
////        }
//////        Bukkit.broadcastMessage("Total checked: " + count);
////    }
//
//    private static void updateDistances(LeafCache leafCache, Location location, @Nullable FakeLeafState state) {
//        count++;
//        final boolean isLog = leafCache.isLogAt(location);/* || addedLogs.contains(location) || removedLogs.contains(location);*/
////        Bukkit.broadcastMessage("Checking " + location + " - is log: " + isLog);
//        final int distance = getLowestDistance(leafCache, location);
//        if (!isLog && state != null && distance == state.actualDistance()) return;
//        if (!isLog && state != null) {
//            state.actualDistance(distance);
//            toSetState.put(location, state);
//        }
//        for (BlockFace face : DIRECTIONS) {
//            final Location relativeLocation = location.clone().add(face.getDirection());
//            final BlockData relative = getBlockData(relativeLocation);
//            FakeLeafState relativeState = leafCache.getAt(relativeLocation);
//            if (relativeState == null && relative instanceof Leaves) {
//                relativeState = leafCache.createAtOrGetAndSet(relativeLocation, relative.getMaterial());
//            }
//            if (relativeState == null) continue;
//            if (currentBlockCount >= MAX_BLOCKS_PER_TICK) {
//                blocksToUpdate.addFirst(new Pair<>(relativeLocation, relative));
//                Bukkit.broadcastMessage("Reached: " + currentBlockCount);
//                return;
//            }
//            currentBlockCount++;
//            updateDistances(leafCache, relativeLocation, relativeState);
//        }
//    }
//
//    private static int getLowestDistance(LeafCache leafCache, Location location) {
//        int distance = 7;
//        for (BlockFace face : DIRECTIONS) {
//            final Location relativeLocation = location.clone().add(face.getDirection());
//            final BlockData relative = getBlockData(relativeLocation);
//            FakeLeafState relativeState = leafCache.getAt(relativeLocation);
//            if (relativeState == null && relative instanceof Leaves) {
//                relativeState = leafCache.createAtOrGetAndSet(relativeLocation, relative.getMaterial());
//            }
//            distance = Math.min(distance, getDistanceAt(leafCache, relativeLocation, relativeState) + 1);
//            if (distance == 1) break;
//        }
//        return distance;
//    }
//
//    private static int getDistanceAt(LeafCache cache, Location location, @Nullable FakeLeafState state) {
//        if (cache.isLogAt(location)) return 0;
//        return state == null ? 7 : state.actualDistance();
//    }
//
//    @Nullable
//    private static ChunkSnapshot getOrCacheSnapshot(Location location) {
//        final int chunkX = location.getBlockX() >> 4;
//        final int chunkZ = location.getBlockZ() >> 4;
//        final World world = location.getWorld();
//        if (world == null || !world.isChunkLoaded(chunkX, chunkZ)) return null;
//        final long chunkKey = ChunkUtil.chunkKeyAt(chunkX, chunkZ);
//        return cachedSnapshots.computeIfAbsent(chunkKey, key -> location.getChunk().getChunkSnapshot());
//    }
//
//    @Nullable
//    private static BlockData getBlockData(Location location) {
//        final ChunkSnapshot snapshot = getOrCacheSnapshot(location);
//        if (snapshot == null) return null;
//        final int x = ChunkUtil.getCoordInChunk(location.getBlockX());
//        final int y = location.getBlockY();
//        final int z = ChunkUtil.getCoordInChunk(location.getBlockZ());
//        return snapshot.getBlockData(x, y, z);
//    }

}
