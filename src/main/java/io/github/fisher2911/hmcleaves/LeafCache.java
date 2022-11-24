package io.github.fisher2911.hmcleaves;

import io.github.fisher2911.hmcleaves.util.ChunkUtil;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LeafCache {

    private final HMCLeaves plugin;
    private final Map<Position2D, Map<Position, FakeLeafState>> cache;
    private final Map<Position2D, Set<Position>> logBlocks;

    public LeafCache(HMCLeaves plugin, Map<Position2D, Map<Position, FakeLeafState>> cache) {
        this.plugin = plugin;
        this.cache = cache;
        this.logBlocks = new ConcurrentHashMap<>();
    }

    @Nullable
    public FakeLeafState getAt(Position2D chunk, Position position) {
        final var map = this.cache.get(chunk);
        if (map == null) return null;
        return map.get(position);
    }

    public void addData(Position2D chunk, Position position, FakeLeafState state) {
        final var map = this.cache.computeIfAbsent(chunk, k -> new ConcurrentHashMap<>());
        if (!this.inBounds(position)) throw new IllegalArgumentException(position + " not in bounds");
        map.put(position, state);
    }

    public boolean isLogAt(Position2D chunk, Position position) {
        final var set = this.logBlocks.get(chunk);
        if (set == null) return false;
        return set.contains(position);
    }

    public void setLogAt(Position2D chunk, Position position) {
        final var set = this.logBlocks.computeIfAbsent(chunk, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
        if (!this.inBounds(position)) throw new IllegalArgumentException(position + " not in bounds");
        set.add(position);
    }

    public void setLogAt(Location location) {
        this.setLogAt(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public void setLogAt(UUID world, int x, int y, int z) {
        final Position2D chunkPos = new Position2D(world, x >> 4, z >> 4);
        final Position position = new Position(ChunkUtil.getCoordInChunk(x), y, ChunkUtil.getCoordInChunk(z));
        this.setLogAt(chunkPos, position);
    }

    public boolean removeLogAt(Position2D chunk, Position position) {
        final var set = this.logBlocks.get(chunk);
        if (set == null) return false;
        return set.remove(position);
    }

    public boolean removeLogAt(Location location) {
        return this.removeLogAt(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public boolean removeLogAt(UUID world, int x, int y, int z) {
        final Position2D chunkPos = new Position2D(world, x >> 4, z >> 4);
        final Position position = new Position(ChunkUtil.getCoordInChunk(x), y, ChunkUtil.getCoordInChunk(z));
        return this.removeLogAt(chunkPos, position);
    }

    private boolean inBounds(Position position) {
        return this.inBounds(position.x()) && this.inBounds(position.z());
    }

    private boolean inBounds(int i) {
        return i >= 0 && i < 16;
    }

    @Nullable
    public FakeLeafState remove(Position2D chunk, Position position) {
        final var map = this.cache.get(chunk);
        if (map == null) return null;
        return map.remove(position);
    }

    public Map<Position, FakeLeafState> getOrAddChunkData(Position2D chunk) {
        return this.cache.computeIfAbsent(chunk, k -> new ConcurrentHashMap<>());
    }

    public void remove(Position2D chunk) {
        this.cache.remove(chunk);
    }

    public FakeLeafState getAt(Location location) {
        final int x = location.getBlockX();
        final int y = location.getBlockY();
        final int z = location.getBlockZ();
        return this.getAt(location.getWorld().getUID(), x, y, z);
    }

    public FakeLeafState getAt(UUID world, int x, int y, int z) {
        final int chunkX = x >> 4;
        final int chunkZ = z >> 4;
        final Position2D chunkPos = new Position2D(world, chunkX, chunkZ);
        final Position position = new Position(ChunkUtil.getCoordInChunk(x), y, ChunkUtil.getCoordInChunk(z));
        return this.getAt(chunkPos, position);
    }

    public boolean isLogAt(Location location) {
        final int x = location.getBlockX();
        final int y = location.getBlockY();
        final int z = location.getBlockZ();
        return this.isLogAt(location.getWorld().getUID(), x, y, z);
    }

    public boolean isLogAt(UUID world, int x, int y, int z) {
        final Position2D chunkPos = new Position2D(world, x >> 4, z >> 4);
        final Position position = new Position(ChunkUtil.getCoordInChunk(x), y, ChunkUtil.getCoordInChunk(z));
        return this.isLogAt(chunkPos, position);
    }

    public void set(UUID world, int x, int y, int z, FakeLeafState state) {
        final Position2D chunkPos = new Position2D(world, x >> 4, z >> 4);
        final Position position = new Position(ChunkUtil.getCoordInChunk(x), y, ChunkUtil.getCoordInChunk(z));
        this.addData(chunkPos, position, state);
    }

    public void set(Location location, FakeLeafState state) {
        final int x = location.getBlockX();
        final int y = location.getBlockY();
        final int z = location.getBlockZ();
        this.set(location.getWorld().getUID(), x, y, z, state);
    }

    public FakeLeafState remove(Location location) {
        return this.remove(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public FakeLeafState remove(UUID world, int x, int y, int z) {
        final Position2D chunkPos = new Position2D(world, x >> 4, z >> 4);
        final Position position = new Position(ChunkUtil.getCoordInChunk(x), y, ChunkUtil.getCoordInChunk(z));
        return this.remove(chunkPos, position);
    }

    public FakeLeafState createAtOrGetAndSet(Location location, Material material) {
        final UUID world = location.getWorld().getUID();
        final int x = location.getBlockX();
        final int y = location.getBlockY();
        final int z = location.getBlockZ();
        final Position2D chunkPos = new Position2D(world, x >> 4, z >> 4);
        final Position position = new Position(ChunkUtil.getCoordInChunk(x), y, ChunkUtil.getCoordInChunk(z));
        return this.getOrAddChunkData(chunkPos).computeIfAbsent(position, k -> this.createDefault(material));
    }

    public FakeLeafState createDefault(Material material) {
        return this.plugin.config().getDefaultState(material);
    }

}
