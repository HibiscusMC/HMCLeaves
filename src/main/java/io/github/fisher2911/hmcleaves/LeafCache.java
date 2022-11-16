package io.github.fisher2911.hmcleaves;

import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LeafCache {

    private final Map<Position2D, Map<Position, FakeLeafState>> cache;
    private final Map<Position2D, Set<Position>> treeBlocks;

    public LeafCache(Map<Position2D, Map<Position, FakeLeafState>> cache) {
        this.cache = cache;
        this.treeBlocks = new HashMap<>();
    }

    @Nullable
    public FakeLeafState getAt(Position2D chunk, Position position) {
        final var map = this.cache.get(chunk);
        if (map == null) return null;
        return map.get(position);
    }

    public void addData(Position2D chunk, Position position, FakeLeafState state) {
        final var map = this.cache.computeIfAbsent(chunk, k -> new HashMap<>());
        if (!this.inBounds(position)) throw new IllegalArgumentException(position + " not in bounds");
        map.put(position, state);
    }

    public boolean isTreeBlock(Position2D chunk, Position position) {
        final var set = this.treeBlocks.get(chunk);
        if (set == null) return false;
        return set.contains(position);
    }

    public void addTreeBlock(Position2D chunk, Position position) {
        final var set = this.treeBlocks.computeIfAbsent(chunk, k -> new HashSet<>());
        if (!this.inBounds(position)) throw new IllegalArgumentException(position + " not in bounds");
        Bukkit.broadcastMessage("Place log at " + chunk.x() * 16 + position.getXInChunk() + " " + position.y() + " " + chunk.y() * 16 + position.getZInChunk());
        set.add(position);
    }

    public void removeTreeBlock(Position2D chunk, Position position) {
        final var set = this.treeBlocks.get(chunk);
        if (set == null) return;
        set.remove(position);
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
        return this.cache.computeIfAbsent(chunk, k -> new HashMap<>());
    }

    public void remove(Position2D chunk) {
        this.cache.remove(chunk);
    }

}
