package com.hibiscusmc.hmcleaves.paper.util;

import com.hibiscusmc.hmcleaves.common.util.MathUtils;
import com.hibiscusmc.hmcleaves.common.util.PositionUtils;
import com.hibiscusmc.hmcleaves.common.world.ChunkPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public static Collection<? extends Player> getNearbyPlays(UUID worldId, ChunkPosition chunkPosition) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> PlayerUtils.playerInViewDistanceOfChunk(player, worldId, chunkPosition.x(), chunkPosition.z()))
                .collect(Collectors.toSet());
    }

}
