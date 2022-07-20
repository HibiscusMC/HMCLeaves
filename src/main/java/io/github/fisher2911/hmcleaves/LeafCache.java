package io.github.fisher2911.hmcleaves;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class LeafCache {

    private final Map<Position2D, Map<Position, WrappedBlockState>> cache;

    public LeafCache(Map<Position2D, Map<Position, WrappedBlockState>> cache) {
        this.cache = cache;
    }

    @Nullable
    public WrappedBlockState getAt(Position2D chunk, Position position) {
        final var map = this.cache.get(chunk);
        if (map == null) return null;
        return map.get(position);
    }

    public void addData(Position2D chunk, Position position, WrappedBlockState state) {
        final var map = this.cache.computeIfAbsent(chunk, k -> new HashMap<>());
        map.put(position, state);
    }

    public void remove(Position2D chunk, Position position) {
        final var map = this.cache.get(chunk);
        if (map == null) return;
        map.remove(position);
    }

    public Map<Position, WrappedBlockState> getOrAddChunkData(Position2D chunk) {
        return this.cache.computeIfAbsent(chunk, k -> new HashMap<>());
    }

    public void remove(Position2D chunk) {
        this.cache.remove(chunk);
    }

}
