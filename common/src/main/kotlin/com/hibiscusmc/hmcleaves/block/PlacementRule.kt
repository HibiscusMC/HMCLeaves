package com.hibiscusmc.hmcleaves.block

import com.hibiscusmc.hmcleaves.world.Position
import org.bukkit.World
import org.bukkit.entity.Player

data class PlacementData(val allowed: Boolean, val data: BlockData)

interface PlacementRule {

    companion object {

        data object ALLOW : PlacementRule {
            override fun canPlace(
                data: BlockData,
                player: Player,
                world: World,
                position: Position
            ): PlacementData {
                return PlacementData(true, data)
            }
        }

    }

    fun canPlace(data: BlockData, player: Player, world: World, position: Position): PlacementData

}
