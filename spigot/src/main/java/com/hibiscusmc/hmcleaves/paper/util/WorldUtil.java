package com.hibiscusmc.hmcleaves.paper.util;

import com.hibiscusmc.hmcleaves.paper.world.Position;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

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

    public static Axis axisFromBlockFace(BlockFace blockFace) {
        return switch (blockFace) {
            case UP, DOWN -> Axis.Y;
            case NORTH, SOUTH -> Axis.X;
            case EAST, WEST -> Axis.Z;
            default -> throw new IllegalArgumentException("Blockface " + blockFace.name() + " does not have an axis");
        };
    }

}
