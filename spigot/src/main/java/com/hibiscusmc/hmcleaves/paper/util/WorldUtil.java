package com.hibiscusmc.hmcleaves.paper.util;

import com.hibiscusmc.hmcleaves.common.world.Position;
import org.bukkit.Location;
import org.bukkit.World;

public final class WorldUtil {

    private WorldUtil() {
        throw new UnsupportedOperationException();
    }

    public static Location convertPosition(World world, Position position) {
        return new Location(
                world,
                position.x(),
                position.y(),
                position.z()
        );
    }

    public static Position convertLocation(Location location) {
        return Position.at(
                location.getWorld().getUID(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }
}
