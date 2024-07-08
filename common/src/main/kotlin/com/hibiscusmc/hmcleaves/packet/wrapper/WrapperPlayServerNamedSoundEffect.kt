package com.hibiscusmc.hmcleaves.packet.wrapper

import com.github.retrooper.packetevents.manager.server.ServerVersion
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.sound.SoundCategory
import com.github.retrooper.packetevents.util.Vector3i
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import java.util.Optional


class WrapperPlayServerNamedSoundEffect(
    private var soundID: Int = 0,
    private var soundName: Optional<String> = Optional.empty(),
    private var fixedSoundRange: Optional<Boolean> = Optional.empty(),
    private var range: Optional<Float> = Optional.empty(),
    private var soundCategory: SoundCategory,
    private var effectPosition: Vector3i,
    private var volume: Float,
    private var pitch: Float
) : PacketWrapper<WrapperPlayServerNamedSoundEffect?>(PacketType.Play.Server.SOUND_EFFECT) {


    private var seed: Long = 0

    override fun read() {
        this.soundID = this.readVarInt()
        if (this.soundID == 0) {
            this.soundName = Optional.of(this.readString())
            this.fixedSoundRange = Optional.of(this.readBoolean())
            if (fixedSoundRange.get()) {
                this.range = Optional.of(this.readFloat())
            } else {
                this.range = Optional.empty()
            }
        } else {
            this.soundName = Optional.empty()
            this.fixedSoundRange = Optional.empty()
            this.range = Optional.empty()
        }
        this.soundCategory = SoundCategory.fromId(this.readVarInt())
        this.effectPosition = Vector3i(this.readInt(), this.readInt(), this.readInt())
        this.volume = this.readFloat()
        this.pitch = this.readFloat()
        if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_19)) {
            this.seed = this.readLong()
        }
    }

    override fun write() {
        this.writeVarInt(this.soundID)
        if (this.soundID == 0) {
            this.writeString(soundName.get())
            this.writeBoolean(fixedSoundRange.get())
            if (fixedSoundRange.get()) {
                this.writeFloat(range.get())
            }
        }
        this.writeVarInt(soundCategory.ordinal)
        this.writeInt(effectPosition.getX() * 8)
        this.writeInt(effectPosition.getY() * 8)
        this.writeInt(effectPosition.getZ() * 8)
        this.writeFloat(this.volume)
        this.writeFloat(this.pitch)
        if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_19)) {
            this.writeLong(this.seed)
        }
    }

    fun getSoundName(): Optional<String> {
        return soundName
    }

    fun setSoundName(soundName: String) {
        this.soundName = Optional.of(soundName)
    }

    fun getFixedSoundRange(): Optional<Boolean> {
        return fixedSoundRange
    }

    fun setFixedSoundRange(fixedSoundRange: Boolean) {
        this.fixedSoundRange = Optional.of(fixedSoundRange)
    }

    fun getRange(): Optional<Float> {
        return range
    }

    fun setRange(range: Float) {
        this.range = Optional.of(range)
    }

    fun getSoundCategory(): SoundCategory {
        return soundCategory
    }

    fun setSoundCategory(soundCategory: SoundCategory) {
        this.soundCategory = soundCategory
    }
}