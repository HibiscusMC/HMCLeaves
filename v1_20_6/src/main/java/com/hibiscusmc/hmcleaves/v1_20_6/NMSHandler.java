package com.hibiscusmc.hmcleaves.v1_20_6;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class NMSHandler implements com.hibiscusmc.hmcleaves.nms.NMSHandler {

    public NMSHandler() {
    }

    @Override
    public byte calculateBreakSpeed(Player player, ItemStack itemStack, Block block, int ticksPassed) {
        final ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        final CraftBlock craftBlock = (CraftBlock) block;
        return getDestroyProgress(serverPlayer, craftBlock.getNMS(), craftBlock.getPosition(), ticksPassed);
    }

    private static byte getDestroyProgress(ServerPlayer player, BlockState state, BlockPos pos, int miningTicks) {
        final float f = state.getDestroyProgress(player, player.level(), pos) * (float) (miningTicks + 1);
        final int k = (int) (f * 10.0F);
        return (byte) k;
    }

}
