package io.github.fisher2911.hmcleaves;

import org.bukkit.Material;

public record LeafData(Material material, int fakeDistance, boolean fakePersistence, boolean actuallyPersistent) {

    @Override
    public String toString() {
        return "LeafData{" +
                "material=" + material +
                ", fakeDistance=" + fakeDistance +
                ", fakePersistence=" + fakePersistence +
                ", actuallyPersistent=" + actuallyPersistent +
                '}';
    }

}
