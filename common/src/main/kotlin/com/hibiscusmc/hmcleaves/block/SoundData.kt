package com.hibiscusmc.hmcleaves.block

import com.github.retrooper.packetevents.protocol.sound.SoundCategory

data class SoundData(val name: String, val soundCategory: SoundCategory, val volume: Float, val pitch: Float)

data class BlockSoundData(val stepSound: SoundData?, val hitSound: SoundData?, val placeSound: SoundData?) {

    companion object {

        val EMPTY = BlockSoundData(null, null, null)

    }

}