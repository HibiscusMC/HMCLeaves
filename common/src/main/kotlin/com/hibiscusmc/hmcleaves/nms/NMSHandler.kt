package com.hibiscusmc.hmcleaves.nms

import com.hibiscusmc.hmcleaves.world.Position
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot

interface NMSHandler {

    fun handleRightClickCustomBlock(player: Player, placePosition: Position, slot: EquipmentSlot)

}