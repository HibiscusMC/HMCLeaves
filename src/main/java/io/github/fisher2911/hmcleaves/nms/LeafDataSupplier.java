package io.github.fisher2911.hmcleaves.nms;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public interface LeafDataSupplier {

    JavaPlugin plugin();

    FakeLeafData getDefault();

    FakeLeafData getAt(UUID world, int x, int y, int z);

    FakeLeafData getAtOrCreate(UUID world, int x, int y, int z);

    void set(UUID world, int x, int y, int z, FakeLeafData data);

    FakeLeafData remove(UUID world, int x, int y, int z);

    boolean isLogAt(UUID world, int x, int y, int z);

}
