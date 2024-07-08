package com.hibiscusmc.hmcleaves.packet.wrapper

import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.util.Vector3i
import com.github.retrooper.packetevents.wrapper.PacketWrapper


class WrapperPlayServerWorldEvent(
    var event: Int,
    var position: Vector3i?,
    var data: Int,
    disableRelativeVolume: Boolean
) : PacketWrapper<WrapperPlayServerWorldEvent>(PacketType.Play.Server.EFFECT) {
    var isDisableRelativeVolume: Boolean = disableRelativeVolume

    override fun read() {
        this.event = this.readInt()
        this.position = this.readBlockPosition()
        this.data = this.readInt()
        this.isDisableRelativeVolume = this.readBoolean()
    }

    override fun write() {
        this.writeInt(this.event)
        this.writeBlockPosition(this.position)
        this.writeInt(this.data)
        this.writeBoolean(this.isDisableRelativeVolume)
    }

    override fun copy(wrapper: WrapperPlayServerWorldEvent) {
        this.event = wrapper.event
        this.position = wrapper.position
        this.data = wrapper.data
        this.isDisableRelativeVolume = wrapper.isDisableRelativeVolume
    }
}