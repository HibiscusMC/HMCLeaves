package com.hibiscusmc.hmcleaves.v1_20_4

import com.hibiscusmc.hmcleaves.nms.NMSHandler
import com.hibiscusmc.hmcleaves.world.Position
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.GameType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
class NMSHandler : NMSHandler {

    override fun handleRightClickCustomBlock(player: Player, placePosition: Position, slot: EquipmentSlot) {
        val itemInHand = player.inventory.itemInMainHand
        val nmsItemInHand = (itemInHand as CraftItemStack).handle.item
        val hand = if (slot === EquipmentSlot.HAND) InteractionHand.MAIN_HAND else InteractionHand.OFF_HAND
        val nmsPlayer = (player as CraftPlayer).handle
        if (nmsPlayer.cooldowns.isOnCooldown(nmsItemInHand)) return
        val hitResult = getPlayerHitResult(nmsPlayer.level(), player.handle)
        val useOnContext = UseOnContext(nmsPlayer, hand, hitResult)
        nmsPlayer.gameMode.useItem(nmsPlayer, nmsPlayer.level(), itemInHand.handle, hand)
        if (nmsItemInHand is BlockItem) {
            nmsItemInHand.place(BlockPlaceContext(useOnContext))
        } else {
            nmsItemInHand.useOn(useOnContext)
        }
    }

    private fun getPlayerHitResult(
        level: Level,
        player: ServerPlayer
    ): BlockHitResult {
        val f1 = player.xRot
        val f2 = player.getYRot()
        val d0 = player.x
        val d1 = player.y + player.eyeHeight.toDouble()
        val d2 = player.z
        val vec3d = Vec3(d0, d1, d2)

        val f3 = Mth.cos(-f2 * 0.017453292f - 3.1415927f)
        val f4 = Mth.sin(-f2 * 0.017453292f - 3.1415927f)
        val f5 = -Mth.cos(-f1 * 0.017453292f)
        val f6 = Mth.sin(-f1 * 0.017453292f)
        val f7 = f4 * f5
        val f8 = f3 * f5
        val d3 = if (player.gameMode.gameModeForPlayer == GameType.CREATIVE) 5.0 else 4.5
        val vec3d1 = vec3d.add(f7.toDouble() * d3, f6.toDouble() * d3, f8.toDouble() * d3)
        return level.clip(ClipContext(vec3d, vec3d1, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player))
    }
}