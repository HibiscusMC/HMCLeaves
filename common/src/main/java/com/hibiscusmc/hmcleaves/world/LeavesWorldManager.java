package com.hibiscusmc.hmcleaves.world;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public final class LeavesWorldManager {

    private final Map<UUID, LeavesWorld> worlds;

    public LeavesWorldManager(Map<UUID, LeavesWorld> worlds) {
        this.worlds = worlds;
    }

    public @Nullable LeavesWorld getWorld(UUID worldId) {
        return this.worlds.get(worldId);
    }

    public LeavesWorld getOrAddWorld(UUID worldId, Function<UUID, LeavesWorld> leavesWorldSupplier) {
        return this.worlds.computeIfAbsent(worldId, leavesWorldSupplier);
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
