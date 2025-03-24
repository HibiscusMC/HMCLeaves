package com.hibiscusmc.hmcleaves.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class PlayerUtils {

    private PlayerUtils() {

    }

    public static boolean playerInViewDistanceOfChunk(Player player, UUID chunkWorldId, int chunkX, int chunkZ) {
        final Location location = player.getLocation();
        if (!chunkWorldId.equals(location.getWorld().getUID())) {
            return false;
        }
        return MathUtils.distanceSquared(
                PositionUtils.coordToChunkCoord(location.getBlockX()),
                PositionUtils.coordToChunkCoord(location.getBlockZ()),
                chunkX,
                chunkZ
        ) <= Math.pow(Bukkit.getViewDistance(), 2);
    }

}
