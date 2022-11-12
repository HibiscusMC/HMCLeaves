package io.github.fisher2911.hmcleaves;

import org.bukkit.Material;

public record LeafData(Material material, int distance, boolean persistent) {

    @Override
    public String toString() {
        return "LeafData{" +
                "material=" + material +
                ", distance=" + distance +
                ", persistent=" + persistent +
                '}';
    }

}
