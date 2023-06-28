/*
 *
 *  *     HMCLeaves
 *  *     Copyright (C) 2022  Hibiscus Creative Studios
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU General Public License
 *  *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package io.github.fisher2911.hmcleaves.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.sound.SoundCategory;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRemoveEntityEffect;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.data.SoundData;
import io.github.fisher2911.hmcleaves.util.Pair;
import io.github.fisher2911.hmcleaves.world.ChunkPosition;
import io.github.fisher2911.hmcleaves.world.Position;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

public class PacketUtils {

    public static WrappedBlockState getState(WrapperPlayServerMultiBlockChange.EncodedBlock block) {
        return block.getBlockState(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
    }

    public static void sendBlockData(BlockData blockData, Position position, Material worldMaterial, Collection<? extends Player> players) {
        final WrappedBlockState state = blockData.getNewState(worldMaterial);
        for (Player player : players) {
            final WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(
                    new Vector3i(position.x(), position.y(), position.z()),
                    state.getGlobalId()
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        }
    }

    public static void sendBlock(Material material, Position position, Collection<? extends Player> players) {
        final int id = SpigotConversionUtil.fromBukkitBlockData(material.createBlockData()).getGlobalId();
        for (Player player : players) {
            final WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(
                    new Vector3i(position.x(), position.y(), position.z()),
                    id
            );
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        }
    }

    public static void sendBlockBreakAnimation(Player player, Location location, int entityId, byte damage) {
        final WrapperPlayServerBlockBreakAnimation packet = new WrapperPlayServerBlockBreakAnimation(
                entityId,
                new Vector3i(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                damage
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(
                player,
                packet
        );
    }

    public static void sendBlockBroken(Player player, Position position, int blockId) {
        final WrapperPlayServerWorldEvent packet = new WrapperPlayServerWorldEvent(
                2001,
                new Vector3i(position.x(), position.y(), position.z()),
                blockId,
                false
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(
                player,
                packet
        );
    }

    public static void sendMiningFatigue(Player player) {
        final WrapperPlayServerEntityEffect packet = new WrapperPlayServerEntityEffect(
                player.getEntityId(),
                PotionTypes.MINING_FATIGUE,
                -1,
                Integer.MAX_VALUE,
                (byte) 0
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(
                player,
                packet
        );
    }

    public static void removeMiningFatigue(Player player) {
        final WrapperPlayServerRemoveEntityEffect packet = new WrapperPlayServerRemoveEntityEffect(
                player.getEntityId(),
                PotionTypes.MINING_FATIGUE
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(
                player,
                packet
        );
    }

    public static void sendArmSwing(Player playerToSwing, Collection<? extends Player> players) {
        for (Player player : players) {
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                    player,
                    new WrapperPlayServerEntityAnimation(
                            playerToSwing.getEntityId(),
                            WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
                    )
            );
        }
    }

    public static void sendMultiBlockChange(
            ChunkPosition chunkPosition,
            Map<Position, BlockData> blocks,
            Map<Position, Material> worldMaterials,
            Collection<? extends Player> players
    ) {
        if (players.isEmpty()) return;
        final Multimap<Integer, Pair<Position, BlockData>> yToBlocksMap = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
        for (var entry : blocks.entrySet()) {
            final Position position = entry.getKey();
            final BlockData blockData = entry.getValue();
            int chunkY = position.y() / 16;
            if (position.y() < 0 && chunkY == 0) chunkY = -1;
            yToBlocksMap.put(chunkY, Pair.of(position, blockData));
        }
        for (Player player : players) {
            for (int y : yToBlocksMap.keySet()) {
                final Collection<Pair<Position, BlockData>> set = yToBlocksMap.get(y);
                final var encodedBlocks = new WrapperPlayServerMultiBlockChange.EncodedBlock[set.size()];
                int i = 0;
                for (var pair : set) {
                    final Position position = pair.getFirst();
                    final BlockData blockData = pair.getSecond();
                    encodedBlocks[i++] = new WrapperPlayServerMultiBlockChange.EncodedBlock(
                            blockData.getNewState(worldMaterials.get(position)),
                            position.x(),
                            position.y(),
                            position.z()
                    );
                }
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerMultiBlockChange(
                                new Vector3i(chunkPosition.x(), y, chunkPosition.z()),
                                false,
                                encodedBlocks
                        )
                );
            }
        }
    }

    public static void sendSound(
            SoundData soundData,
            Position position,
            Collection<? extends Player> players
    ) {
        sendSound(
                soundData.name(),
                soundData.soundCategory(),
                soundData.volume(),
                soundData.pitch(),
                position,
                players
        );
    }

    public static void sendSound(
            String name,
            SoundCategory soundCategory,
            float volume,
            float pitch,
            Position position,
            Collection<? extends Player> players
    ) {
        final Vector3i effectPosition = new Vector3i(position.x(), position.y(), position.z());
        final WrapperPlayServerNamedSoundEffect packet = new WrapperPlayServerNamedSoundEffect(
                0,
                Optional.of(name),
                Optional.of(false),
                Optional.empty(),
                soundCategory,
                effectPosition,
                volume,
                pitch
        );
        final PlayerManager playerManager = PacketEvents.getAPI().getPlayerManager();
        for (Player player : players) {
            Bukkit.broadcastMessage("Sent sound to " + player.getName() + " " + name + " " + soundCategory + " " + volume + " " + pitch);
            playerManager.sendPacket(player, packet);
        }
    }

}
