package io.github.fisher2911.hmcleaves.util;

import io.github.fisher2911.hmcleaves.Config;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.LeafCache;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class LeafUtil {

    private static final HMCLeaves PLUGIN = HMCLeaves.getPlugin(HMCLeaves.class);

    private static final BlockFace[] DIRECTIONS = new BlockFace[]{
            BlockFace.WEST,
            BlockFace.EAST,
            BlockFace.DOWN,
            BlockFace.UP,
            BlockFace.NORTH,
            BlockFace.SOUTH
    };


    public static void checkPlaceLeaf(Block block, LeafCache cache) {
        checkLeaf(block, cache, true);
    }

    public static void checkBreakLeaf(Block block, LeafCache cache) {
        cache.remove(block.getLocation());
        checkLeaf(block, cache, false);
    }

    private static void checkLeaf(
            Block block,
            LeafCache cache,
            boolean isLeafPlace
    ) {

    }

    public static void placeLog(
            Block block,
            LeafCache cache,
            Config config
    ) {

    }

    public static void placeLog(
            Block block,
            Config config,
            LeafCache cache
    ) {

    }


    public static void breakLog(
            Block block,
            LeafCache cache
    ) {

    }

    public static void updateAll() {

    }

//    private static void breakLogCheck(Block block, LeafCache cache) {
//        for (BlockFace face : DIRECTIONS) {
//            final Block relative = block.getRelative(face);
//            if (!Tag.LEAVES.isTagged(relative.getType())) continue;
//            bfs(relative, cache);
//        }
//        updateOnConnectorBlockBreak(block, cache);
//    }

//    private static void updateOnConnectorBlockBreak(Block broken, LeafCache cache) {
//        for (BlockFace face : DIRECTIONS) {
//            final Block relative = broken.getRelative(face);
//            final Block log = bfsFindLog(relative, cache);
//            if (log == null) continue;
//            updateLeavesFromLog(log, cache);
//        }
//    }

//    private static void updateLeavesFromLog(Block log, LeafCache cache) {
//        for (BlockFace face : DIRECTIONS) {
//            final Block relative = log.getRelative(face);
//            if (!Tag.LEAVES.isTagged(relative.getType())) continue;
//            bfs(relative, cache);
//        }
//    }

//    @Nullable
//    private static Block bfsFindLog(Block block, LeafCache cache) {
//        final Queue<Block> queue = new LinkedList<>();
//        final Set<Block> checked = new HashSet<>();
//        queue.add(block);
//        while (!queue.isEmpty()) {
//            final Block current = queue.poll();
//            if (checked.contains(current)) continue;
//            final Location currentLocation = current.getLocation();
//            final Position2D currentChunkPos = new Position2D(currentLocation.getWorld().getUID(), currentLocation.getChunk().getX(), currentLocation.getChunk().getZ());
//            final Position currentPosition = new Position(ChunkUtil.getCoordInChunk(currentLocation.getBlockX()), currentLocation.getBlockY(), ChunkUtil.getCoordInChunk(currentLocation.getBlockZ()));
//            checked.add(current);
//            if (cache.isTreeBlock(currentChunkPos, currentPosition)) {
//                Bukkit.broadcastMessage("Found log at " + current.getX() + " " + current.getY() + " " + current.getZ());
//                return current;
//            }
//            for (BlockFace face : DIRECTIONS) {
//                final Block relative = current.getRelative(face);
//                final Location relativeLocation = relative.getLocation();
//                final Position2D chunkPos = new Position2D(relativeLocation.getWorld().getUID(), relativeLocation.getChunk().getX(), relativeLocation.getChunk().getZ());
//                final Position position = new Position(ChunkUtil.getCoordInChunk(relativeLocation.getBlockX()), relativeLocation.getBlockY(), ChunkUtil.getCoordInChunk(relativeLocation.getBlockZ()));
//                if (cache.isTreeBlock(chunkPos, position)) {
//                    Bukkit.broadcastMessage("Found log at " + relative.getX() + " " + relative.getY() + " " + relative.getZ());
//                    return relative;
//                }
//                if (Tag.LEAVES.isTagged(relative.getType())) {
//                    queue.add(relative);
//                }
//            }
//        }
//        for (Block checkedBlock : checked) {
//            final Location checkedLocation = checkedBlock.getLocation();
//            final Position2D chunkPos = new Position2D(checkedLocation.getWorld().getUID(), checkedLocation.getChunk().getX(), checkedLocation.getChunk().getZ());
//            final Position position = new Position(ChunkUtil.getCoordInChunk(checkedLocation.getBlockX()), checkedLocation.getBlockY(), ChunkUtil.getCoordInChunk(checkedLocation.getBlockZ()));
//            final FakeLeafState state = cache.getAt(chunkPos, position);
//            if (state == null) continue;
//            state.actualDistance(7);
//            if (state.actuallyPersistent()) continue;
//            final Leaves leaves = (Leaves) checkedBlock.getBlockData();
//            leaves.setPersistent(false);
//            checkedBlock.setBlockData(leaves, false);
//        }
//        return null;
//    }

//    private static void bfsSearchConnectedLogs(Block block, LeafCache cache, boolean isLeafPlace) {
//        final Queue<Block> queue = new LinkedList<>();
//        final Set<Block> checked = new HashSet<>();
//        final Set<Block> logs = new HashSet<>();
//        final Map<Block, FakeLeafState> checkedLeaves = new HashMap<>();
//        if (isLeafPlace) {
//            final Location location = block.getLocation();
//            final Position2D chunkPos = new Position2D(location.getWorld().getUID(), location.getChunk().getX(), location.getChunk().getZ());
//            final Position position = new Position(ChunkUtil.getCoordInChunk(location.getBlockX()), location.getBlockY(), ChunkUtil.getCoordInChunk(location.getBlockZ()));
//            final FakeLeafState state = cache.getAt(chunkPos, position);
//            if (state != null) {
//                if (checkAllConnectedLower(block, state, cache)) {
//                    return;
//                }
//            }
//        }
//        queue.add(block);
//        while (!queue.isEmpty()) {
//            final Block current = queue.poll();
//            if (checked.contains(current)) continue;
//            final Location currentLocation = current.getLocation();
//            final Position2D currentChunkPos = new Position2D(currentLocation.getWorld().getUID(), currentLocation.getChunk().getX(), currentLocation.getChunk().getZ());
//            final Position currentPosition = new Position(ChunkUtil.getCoordInChunk(currentLocation.getBlockX()), currentLocation.getBlockY(), ChunkUtil.getCoordInChunk(currentLocation.getBlockZ()));
//            checked.add(current);
//            if (cache.isTreeBlock(currentChunkPos, currentPosition)) {
//                logs.add(current);
//            } else {
//                final FakeLeafState state = cache.getAt(currentChunkPos, currentPosition);
//                if (state != null) {
//                    checkedLeaves.put(current, state);
//                }
//            }
//            for (BlockFace face : DIRECTIONS) {
//                final Block relative = current.getRelative(face);
//                final Location relativeLocation = relative.getLocation();
//                final Position2D chunkPos = new Position2D(relativeLocation.getWorld().getUID(), relativeLocation.getChunk().getX(), relativeLocation.getChunk().getZ());
//                final Position position = new Position(ChunkUtil.getCoordInChunk(relativeLocation.getBlockX()), relativeLocation.getBlockY(), ChunkUtil.getCoordInChunk(relativeLocation.getBlockZ()));
//                if (cache.isTreeBlock(chunkPos, position)) {
//                    logs.add(relative);
//                    queue.add(relative);
//                    continue;
//                }
//                final FakeLeafState state = cache.getAt(chunkPos, position);
//                if (state == null) continue;
//                state.actualDistance(7);
//                queue.add(relative);
//                checkedLeaves.put(relative, state);
//            }
//        }
//        bfsFromLogs(logs, cache, checkedLeaves);
////        return logs;
//    }
//
//    private static boolean checkAllConnectedLower(Block block, FakeLeafState state, LeafCache cache) {
//        int lowest = 7;
//        boolean found = false;
//        for (BlockFace face : DIRECTIONS) {
//            final Block relative = block.getRelative(face);
//            final Location relativeLocation = relative.getLocation();
//            final Position2D chunkPos = new Position2D(relativeLocation.getWorld().getUID(), relativeLocation.getChunk().getX(), relativeLocation.getChunk().getZ());
//            final Position position = new Position(ChunkUtil.getCoordInChunk(relativeLocation.getBlockX()), relativeLocation.getBlockY(), ChunkUtil.getCoordInChunk(relativeLocation.getBlockZ()));
//            if (cache.isTreeBlock(chunkPos, position)) {
//                lowest = 0;
//                continue;
//            }
//            final FakeLeafState relativeState = cache.getAt(chunkPos, position);
//            if (relativeState == null) continue;
//            lowest = Math.min(lowest, relativeState.actualDistance());
//            if (relativeState.actualDistance() < 5) continue;
//            found = true;
//        }
//        if (!found) {
//            state.actualDistance(Math.min(lowest + 1, 7));
//            Bukkit.broadcastMessage("All lower");
//            if (state.actualDistance() < 7) return true;
//            if (block.getBlockData() instanceof final Leaves leaves && !state.actuallyPersistent()) {
//                leaves.setPersistent(false);
//                block.setBlockData(leaves, false);
//            }
//            return true;
//        }
//        state.actualDistance(7);
//        return false;
//    }
//
//    private static void bfsFromLogs(Set<Block> logs, LeafCache cache, Map<Block, FakeLeafState> connectedLeaves) {
//        for (Block log : logs) {
//            bfs(log, cache, connectedLeaves);
//        }
//        Bukkit.broadcastMessage("Logs size: " + logs.size() + " connected size: " + connectedLeaves.size());
//        connectedLeaves.forEach((block, state) -> {
//            if (state.actuallyPersistent()) return;
//            if (!(block.getBlockData() instanceof final Leaves leaves)) return;
//            leaves.setPersistent(false);
//            block.setBlockData(leaves, false);
//        });
//    }
//
//    private static void updateConnected(Block start, LeafCache cache) {
////        bfsFromLogs(bfsSearchConnectedLogs());
//    }
//
//    private static void bfs(Block block, LeafCache cache, Map<Block, FakeLeafState> connectedLeaves) {
//        final LinkedList<Block> queue = new LinkedList<>();
//        final Set<Block> checked = new HashSet<>();
//        queue.add(block);
////        connectedLeaves.add(block);
////        boolean isFirst = true;
//        int count = 0;
//        while (!queue.isEmpty()) {
//            final Block current = queue.poll();
//            final Location currentLocation = current.getLocation();
//            final UUID world = current.getWorld().getUID();
//            final Position2D chunkPos = new Position2D(world, currentLocation.getChunk().getX(), currentLocation.getChunk().getZ());
//            final Position position = new Position(ChunkUtil.getCoordInChunk(currentLocation.getBlockX()), currentLocation.getBlockY(), ChunkUtil.getCoordInChunk(currentLocation.getBlockZ()));
//            final FakeLeafState state = cache.getAt(chunkPos, position);
//            if (checked.contains(current)) continue;
//            checked.add(current);
//            boolean isLog = false;
//            if (state == null) {
//                if (!cache.isTreeBlock(chunkPos, position)) continue;
//                isLog = true;
//            }
////            if (!isLog) {
////                connectedLeaves.put(current, state);
////            }
//            for (BlockFace blockFace : DIRECTIONS) {
//                final Block relative = current.getRelative(blockFace);
////                if (!connectedLeaves.containsKey(relative)) continue;
//                final Location relativeLocation = relative.getLocation();
//                final Position2D relativeChunkPos = new Position2D(world, relativeLocation.getChunk().getX(), relativeLocation.getChunk().getZ());
//                final Position relativePosition = new Position(ChunkUtil.getCoordInChunk(relativeLocation.getBlockX()), relativeLocation.getBlockY(), ChunkUtil.getCoordInChunk(relativeLocation.getBlockZ()));
//                if (cache.isTreeBlock(relativeChunkPos, relativePosition)) {
//                    if (isLog) continue;
//                    state.actualDistance(1);
//                    if (block.getBlockData() instanceof final Leaves leaves) {
//                        if (!leaves.isPersistent()) {
//                            leaves.setPersistent(true);
//                            block.setBlockData(leaves, false);
//                        }
//                        final CustomBlockData customBlockData = new CustomBlockData(block, PLUGIN);
//                        customBlockData.set(PDCUtil.ACTUAL_DISTANCE_KEY, PersistentDataType.BYTE, (byte) state.actualDistance());
//                    }
//                    count++;
//                    connectedLeaves.remove(current);
//                    continue;
//                }
//                if (!(relative.getBlockData() instanceof final Leaves leaves)) continue;
//                final FakeLeafState relativeState = cache.getAt(relativeChunkPos, relativePosition);
//                if (relativeState == null) continue;
//                if (isLog) {
//                    relativeState.actualDistance(1);
//                    final CustomBlockData customBlockData = new CustomBlockData(relative, PLUGIN);
//                    customBlockData.set(PDCUtil.ACTUAL_DISTANCE_KEY, PersistentDataType.BYTE, (byte) relativeState.actualDistance());
//                    if (!leaves.isPersistent()) {
//                        leaves.setPersistent(true);
//                        relative.setBlockData(leaves, false);
//                    }
//                    connectedLeaves.remove(relative);
//                    queue.add(relative);
//                    count++;
//                    continue;
//                }
//                if (relativeState.actualDistance() > state.actualDistance()) {
//                    relativeState.actualDistance(Math.min(7, state.actualDistance() + 1));
//                    final CustomBlockData customBlockData = new CustomBlockData(relative, PLUGIN);
//                    customBlockData.set(PDCUtil.ACTUAL_DISTANCE_KEY, PersistentDataType.BYTE, (byte) relativeState.actualDistance());
//                    if (relativeState.actualDistance() <= 6) {
//                        connectedLeaves.remove(relative);
//                        if (!leaves.isPersistent()) {
//                            leaves.setPersistent(true);
//                            relative.setBlockData(leaves, false);
//                        }
//                        count++;
//                    }
//                    if (relativeState.actualDistance() >= 6) continue;
//                    queue.add(relative);
//                    continue;
//                }
////                if (relativeState.actualDistance() < state.actualDistance() + 1) {
////                    state.actualDistance(relativeState.actualDistance() + 1);
////                    queue.add(relative);
////                    relativeState.actualDistance(state.actualDistance() + (isLog ? 0 : 1));
////                }
//            }
//        }
//        Bukkit.broadcastMessage("Count: " + count);
////        connectedLeaves.forEach((b, state) -> {
////            final Location location = b.getLocation();
////            final UUID world = location.getWorld().getUID();
//////            final Position2D chunkPos = new Position2D(world, location.getChunk().getX(), location.getChunk().getZ());
//////            final Position position = new Position(ChunkUtil.getCoordInChunk(location.getBlockX()), location.getBlockY(), ChunkUtil.getCoordInChunk(location.getBlockZ()));
//////            final FakeLeafState newState = cache.getAt(chunkPos, position);
////            if (state == null) return;
////            if (state.actualDistance() >= 7 && !state.actuallyPersistent()) {
////                final Leaves leaves = (Leaves) b.getBlockData();
////                leaves.setPersistent(false);
////                b.setBlockData(leaves, false);
////            }
////            final CustomBlockData customBlockData = new CustomBlockData(b, PLUGIN);
////        });
//    }
//
//    private static void tick(HMCLeaves plugin, LeafCache cache, Block block) {
//        Bukkit.broadcastMessage("Ticking");
//        final FakeLeafState state = cache.getAt(block.getLocation());
//        if (state == null) return;
//        updateShape(plugin, cache, state, block);
//        Bukkit.broadcastMessage("Updating");
//        for (BlockFace direction : DIRECTIONS) {
//            final FakeLeafState relativeState = cache.getAt(block.getRelative(direction).getLocation());
//            if (relativeState == null) continue;
//            updateDistance(plugin, block.getRelative(direction), relativeState, cache);
//        }
////        Bukkit.getScheduler().runTaskLater(plugin, () -> updateDistance(plugin, block, state, cache), 1);
//    }
//
//    private static void updateOnLogAction(HMCLeaves plugin, LeafCache cache, Block block) {
//        for (BlockFace face : DIRECTIONS) {
//            final Block relative = block.getRelative(face);
//            if (!(relative.getBlockData() instanceof final Leaves leaves)) continue;
//            final FakeLeafState state = cache.getAt(relative.getLocation());
//            if (state == null) continue;
//            tick(PLUGIN, cache, block, null);
////            updateDistance(plugin, relative, state, cache);
//        }
//    }
//
//    private static void updateShape(HMCLeaves plugin, LeafCache cache, @NotNull FakeLeafState state, Block block, Block neighbor) {
////        final UUID world = block.getWorld().getUID();
//
//        int i = getDistanceAt(cache, neighbor) + 1;
//
//        if (i != 1 || state.actualDistance() != i) {
//            Bukkit.getScheduler().runTaskLater(plugin, () -> tick(plugin, cache, block, state), 1);
//        }
//    }
//
//    private static void updateDistance(HMCLeaves plugin, Block block, @Nullable FakeLeafState state, LeafCache cache) {
//        int distance = 7;
//        for (BlockFace direction : DIRECTIONS) {
//            distance = Math.min(distance, getDistanceAt(cache, block.getRelative(direction)) + 1);
//            if (distance == 1) break;
//        }
//
//        Bukkit.broadcastMessage("New distance: " + distance + " previous: " + (state == null ? "" : state.actualDistance()));
//
//        final PersistentDataContainer blockData = new CustomBlockData(
//                block,
//                plugin
//        );
//        int previousDistance = state == null ? 7 : state.actualDistance();
//        if (previousDistance != distance) {
//            if (state != null) {
//                final FakeLeafState snapshot = state.snapshot();
//                Bukkit.getScheduler().runTaskLater(plugin, () -> tick(plugin, cache, block, snapshot), 1);
//            }
//        }
//        if (state == null) return;
////        return state.setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE, data.fakeDistance());
//        state.actualDistance(distance);
//        final WrappedBlockState wrappedState = state.state();
//        if (wrappedState.getDistance() != distance) {
//            PDCUtil.setDistance(blockData, (byte) wrappedState.getDistance());
//        }
//        if (distance == 7 && !state.actuallyPersistent() && block.getBlockData() instanceof final Leaves leaves) {
//            PDCUtil.setActualPersistent(blockData, (byte) 0);
//            leaves.setPersistent(false);
//            block.setBlockData(leaves, false);
//        }
//    }
//
//
//    private static void scheduleTick(LeafCache cache, Block block) {
//        Bukkit.getScheduler().runTaskLater(PLUGIN, () -> tick(cache, block), 1);
//    }
//
//    private static void setBlock(LeafCache cache, Block block, @Nullable FakeLeafState setTo) {
//        final FakeLeafState current = cache.getAt(block.getLocation());
//        if (current == null && setTo == null) return;
//        if (current == null ||
//                setTo == null ||
//                setTo.actualDistance() != current.actualDistance() ||
//                setTo.actuallyPersistent() != current.actuallyPersistent()
//        ) {
//            for (BlockFace face : DIRECTIONS) {
//                final FakeLeafState relativeState = cache.getAt(block.getRelative(face).getLocation());
//                final Block relative = block.getRelative(face);
//                final Location relativeLocation = relative.getLocation();
//                if (relativeState == null && !cache.isLogAt(relativeLocation)) continue;
//                updateShape(cache, setTo, relative, block);
//            }
//        }
//    }
//
//    public static void tick(LeafCache cache, Block block) {
//        final FakeLeafState state = cache.getAt(block.getLocation());
//        if (state == null) return;
//        final int previousDistance = state.actualDistance();
//        state.actualDistance(getDistanceAt(cache, block));
//        if (previousDistance != state.actualDistance() && state.actualDistance() == 7 && !state.actuallyPersistent()) {
//            final CustomBlockData blockData = new CustomBlockData(block, PLUGIN);
//            PDCUtil.setActualPersistent(blockData, (byte) 0);
//            final Leaves leaves = (Leaves) block.getBlockData();
//            leaves.setPersistent(false);
//            block.setBlockData(leaves, false);
//        }
//        setBlock(cache, block, cache.getAt(block.getLocation()));
//    }
//
//    public static FakeLeafState updateShape(LeafCache cache, FakeLeafState state, Block neighbor, Block block) {
//
//        int i = getDistanceAt(cache, neighbor) + 1;
//
//        if (i != 1 || state.actualDistance() != i) {
//            scheduleTick(cache, block);
//        }
//
//        return state;
//    }
//


//    public static void updateDistances(LeafCache cache, Block block, @Nullable FakeLeafState state) {
//        final boolean isLog = cache.isLogAt(block.getLocation());
//        if (!isLog && state == null) {
//            Bukkit.broadcastMessage("Return 1");
//            return;
//        }
//        final int distance = getLowestDistance(cache, block, state);
//        if (!isLog && distance == state.actualDistance()) {
//            Bukkit.broadcastMessage("Return 2: " + distance);
//            return;
//        }
//        Bukkit.broadcastMessage("Updating distance to " + distance);
//        if (!isLog) state.actualDistance(distance);
//        for (BlockFace face : DIRECTIONS) {
//            final Block relative = block.getRelative(face);
//            final Location relativeLocation = relative.getLocation();
//            FakeLeafState relativeState = cache.getAt(relativeLocation);
//            if (relativeState == null && relative.getBlockData() instanceof Leaves) {
//                relativeState = cache.createAtOrGetAndSet(relativeLocation, relative.getType());
//            }
////            if (relativeState == null && !(relative.getBlockData() instanceof Leaves)) continue;
//            updateDistances(cache, relative, relativeState);
//        }
//    }
//
//    private static int getLowestDistance(LeafCache cache, Block block, @Nullable FakeLeafState state) {
//        int distance = 7;
//        for (BlockFace face : DIRECTIONS) {
//            final Block relative = block.getRelative(face);
//            final Location relativeLocation = relative.getLocation();
//            FakeLeafState relativeState = cache.getAt(relativeLocation);
//            if (relativeState == null && relative.getBlockData() instanceof Leaves) {
//                relativeState = cache.createAtOrGetAndSet(relativeLocation, relative.getType());
//            }
//            distance = Math.min(distance, getDistanceAt(cache, relative, relativeState) + 1);
//        }
//        return distance;
//    }
//
//    private static int getDistanceAt(LeafCache cache, Block block, @Nullable FakeLeafState state) {
//        if (Tag.LOGS.isTagged(block.getType())) return 0;
//        if (cache.isLogAt(block.getLocation())) return 0;
//        return state == null ? 7 : state.actualDistance();
//    }

}
