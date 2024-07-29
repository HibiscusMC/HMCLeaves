package com.hibiscusmc.hmcleaves.block

import com.hibiscusmc.hmcleaves.item.BlockDrops
import com.hibiscusmc.hmcleaves.item.ItemSupplier
import com.hibiscusmc.hmcleaves.packet.mining.BlockBreakModifier
import com.hibiscusmc.hmcleaves.texture.TextureData

data class BlockMechanics(
    val blockFamily: BlockFamily,
    val blockSoundData: BlockSoundData,
    val itemSupplier: ItemSupplier,
    val blockDrops: BlockDrops,
    val settings: BlockSettings,
    val placeConditions: List<PlaceConditions>,
    val textureData: TextureData? = null,
    val blockBreakModifier: BlockBreakModifier? = null,
)