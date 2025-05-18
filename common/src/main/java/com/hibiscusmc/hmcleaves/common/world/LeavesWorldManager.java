package com.hibiscusmc.hmcleaves.common.world;

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
        return this.worlds.computeIfAbsent(worldId, worldCreator);
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

}
