package com.hibiscusmc.hmcleaves.paper.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlock;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlockState;
import com.hibiscusmc.hmcleaves.paper.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorld;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorldManager;
import com.hibiscusmc.hmcleaves.paper.world.Position;
import com.hibiscusmc.hmcleaves.paper.breaking.BlockBreakManager;
import com.hibiscusmc.hmcleaves.paper.config.LeavesConfig;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class PlayerDigListener extends PacketListenerAbstract {

    private final LeavesWorldManager leavesWorldManager;
    private final BlockBreakManager blockBreakManager;
    private final LeavesConfig config;

    public PlayerDigListener(BlockBreakManager blockBreakManager, LeavesWorldManager leavesWorldManager, LeavesConfig config) {
        this.blockBreakManager = blockBreakManager;
        this.leavesWorldManager = leavesWorldManager;
        this.config = config;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.getPlayer() instanceof final Player player)) return;
        if (!this.config.isWorldWhitelisted(player.getWorld().getUID())) return;
        final PacketTypeCommon packetType = event.getPacketType();
        if (packetType == PacketType.Play.Client.PLAYER_DIGGING) {
            this.handlePlayerDigging(event, player.getWorld().getUID());
        }
    }

    private void handlePlayerDigging(PacketReceiveEvent event, UUID world) {
        final WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
        if (!(event.getPlayer() instanceof final Player player)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;
        final DiggingAction diggingAction = packet.getAction();
        final Vector3i blockPosition = packet.getBlockPosition();
        final Position position = Position.at(
                world,
                blockPosition.getX(),
                blockPosition.getY(),
                blockPosition.getZ()
        );
        final LeavesWorld leavesWorld = this.leavesWorldManager.getWorld(world);
        if (leavesWorld == null) {
            return;
        }
        final LeavesChunk leavesChunk = leavesWorld.getChunk(position.toChunkPosition());
        if (leavesChunk == null) {
            return;
        }
        final CustomBlockState customBlockState = leavesChunk.getBlock(position);
        if (customBlockState == null) {
            return;
        }
        if (diggingAction == DiggingAction.START_DIGGING) {
            PacketUtil.sendMiningFatigue(player);
            this.blockBreakManager.startBlockBreak(
                    player,
                    position,
                    customBlockState
            );
            return;
        }
        if (diggingAction == DiggingAction.CANCELLED_DIGGING) {
            this.blockBreakManager.cancelBlockBreak(player);
            PacketUtil.removeMiningFatigue(player);
            return;
        }
        if (diggingAction == DiggingAction.FINISHED_DIGGING) {
            this.blockBreakManager.cancelBlockBreak(player);
            PacketUtil.removeMiningFatigue(player);
        }
    }

}
