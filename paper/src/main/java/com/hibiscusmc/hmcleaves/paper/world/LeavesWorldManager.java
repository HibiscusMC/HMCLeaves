package com.hibiscusmc.hmcleaves.paper.world;

import com.hibiscusmc.hmcleaves.paper.block.CustomBlock;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlockState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class LeavesWorldManager {

    private final Map<UUID, LeavesWorld> worlds;
    private final Function<UUID, LeavesWorld> worldCreator;

    public LeavesWorldManager(Map<UUID, LeavesWorld> worlds, Function<UUID, LeavesWorld> worldCreator) {
        this.worlds = worlds;
        this.worldCreator = worldCreator;
    }

    public @Nullable LeavesWorld getWorld(UUID worldId) {
        return this.worlds.get(worldId);
    }

    public LeavesWorld getOrAddWorld(UUID worldId) {
        return this.worlds.computeIfAbsent(worldId, this.worldCreator);
    }

    public void addWorld(UUID worldId, LeavesWorld world) {
        this.worlds.put(worldId, world);
    }

    public @Nullable LeavesWorld removeWorld(UUID worldId) {
        return this.worlds.remove(worldId);
    }

    public boolean isEmpty() {
        return this.worlds.isEmpty();
    }

    public @UnmodifiableView Map<UUID, LeavesWorld> getWorlds() {
        return Collections.unmodifiableMap(this.worlds);
    }

    public @Nullable CustomBlockState removeBlock(UUID worldId, Position position) {
        final LeavesWorld world = this.worlds.get(worldId);
        if (world == null) {
            return null;
        }
        final LeavesChunk leavesChunk = world.getChunk(position.toChunkPosition());
        if (leavesChunk == null) {
            return null;
        }
        return leavesChunk.removeBlock(position);
    }

}
