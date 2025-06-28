package com.hibiscusmc.hmcleaves.paper.packet;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

public final class WrapperPlayServerWorldEvent extends PacketWrapper<WrapperPlayServerWorldEvent> {

    private int event;
    private Vector3i position;
    private int data;
    private boolean disableRelativeVolume;

    public WrapperPlayServerWorldEvent(PacketSendEvent event) {
        super(event);
    }

    public WrapperPlayServerWorldEvent(int event, Vector3i position, int data, boolean disableRelativeVolume) {
        super(PacketType.Play.Server.EFFECT);
        this.event = event;
        this.position = position;
        this.data = data;
        this.disableRelativeVolume = disableRelativeVolume;
    }

    public void read() {
        this.event = this.readInt();
        this.position = this.readBlockPosition();
        this.data = this.readInt();
        this.disableRelativeVolume = this.readBoolean();
    }

    public void write() {
        this.writeInt(this.event);
        this.writeBlockPosition(this.position);
        this.writeInt(this.data);
        this.writeBoolean(this.disableRelativeVolume);
    }

    public void copy(WrapperPlayServerWorldEvent wrapper) {
        this.event = wrapper.event;
        this.position = wrapper.position;
        this.data = wrapper.data;
        this.disableRelativeVolume = wrapper.disableRelativeVolume;
    }

    public int getEvent() {
        return event;
    }

    public void setEvent(int event) {
        this.event = event;
    }

    public Vector3i getPosition() {
        return position;
    }

    public void setPosition(Vector3i position) {
        this.position = position;
    }

    public int getData() {
        return data;
    }

    public void setData(int data) {
        this.data = data;
    }

    public boolean isDisableRelativeVolume() {
        return disableRelativeVolume;
    }

    public void setDisableRelativeVolume(boolean disableRelativeVolume) {
        this.disableRelativeVolume = disableRelativeVolume;
    }

}