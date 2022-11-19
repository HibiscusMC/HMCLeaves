package io.github.fisher2911.hmcleaves;

import io.github.fisher2911.hmcleaves.nms.FakeLeafData;
import io.github.fisher2911.hmcleaves.nms.LeafDataSupplier;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import io.github.fisher2911.hmcleaves.util.PositionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class LeafCache implements LeafDataSupplier {

    private final Supplier<HMCLeaves> pluginSupplier;
    private HMCLeaves plugin;
    private final Map<Position2D, Map<Position, FakeLeafData>> cache;

    public LeafCache(Supplier<HMCLeaves> pluginSupplier, Map<Position2D, Map<Position, FakeLeafData>> cache) {
        this.pluginSupplier = pluginSupplier;
        this.cache = cache;
    }

    @Nullable
    public FakeLeafData getAt(Position2D chunk, Position position) {
        final var map = this.cache.get(chunk);
        if (map == null) return null;
        return map.get(position);
    }

    public void addData(Position2D chunk, Position position, FakeLeafData data) {
        final var map = this.cache.computeIfAbsent(chunk, k -> new HashMap<>());
        if (!this.inBounds(position)) throw new IllegalArgumentException(position + " not in bounds");
        map.put(position, data);
    }

    private boolean inBounds(Position position) {
        return this.inBounds(position.x()) && this.inBounds(position.z());
    }

    private boolean inBounds(int i) {
        return i >= 0 && i < 16;
    }

    @Nullable
    public FakeLeafData remove(Position2D chunk, Position position) {
        final var map = this.cache.get(chunk);
        if (map == null) return null;
        return map.remove(position);
    }

    public Map<Position, FakeLeafData> getOrAddChunkData(Position2D chunk) {
        return this.cache.computeIfAbsent(chunk, k -> new HashMap<>());
    }

    public void remove(Position2D chunk) {
        this.cache.remove(chunk);
    }

    public void remove(Chunk chunk) {
        this.remove(new Position2D(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ()));
    }

    @Override
    public FakeLeafData getAt(UUID world, int x, int y, int z) {
        final int chunkX = x >> 4;
        final int chunkZ = z >> 4;
        final Position2D chunkPos = new Position2D(world, chunkX, chunkZ);
        final Position position = new Position(PositionUtil.getCoordInChunk(x), y, PositionUtil.getCoordInChunk(z));
        return this.getAt(chunkPos, position);
    }

    @Override
    public FakeLeafData getAtOrCreate(UUID world, int x, int y, int z) {
        final FakeLeafData data = this.getAt(world, x, y, z);
        if (data != null) return data;
        final FakeLeafData fakeLeafData = this.plugin().config().getDefaultData();
        this.set(world, x, y, z, fakeLeafData);
        return fakeLeafData;
    }

    @Override
    public boolean isLogAt(UUID worldUUID, int x, int y, int z) {
        final World world = Bukkit.getWorld(worldUUID);
        if (world == null) return false;
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return false;
        final Block block = world.getBlockAt(x, y, z);
        return this.plugin().config().isLogBlock(block);
    }

    @Override
    public HMCLeaves plugin() {
        if (this.plugin == null) this.plugin = this.pluginSupplier.get();
        return this.plugin;
    }

    @Override
    public FakeLeafData getDefault() {
        return this.plugin().config().getDefaultData();
    }

    @Override
    public void set(UUID world, int x, int y, int z, FakeLeafData data) {
        final Position2D chunkPos = new Position2D(world, x >> 4, z >> 4);
        final Position position = new Position(PositionUtil.getCoordInChunk(x), y, PositionUtil.getCoordInChunk(z));
        this.addData(chunkPos, position, data);
    }

    @Override
    public void remove(UUID world, int x, int y, int z) {
        final Position2D chunkPos = new Position2D(world, x >> 4, z >> 4);
        final Position position = new Position(PositionUtil.getCoordInChunk(x), y, PositionUtil.getCoordInChunk(z));
        this.remove(chunkPos, position);
    }

}
