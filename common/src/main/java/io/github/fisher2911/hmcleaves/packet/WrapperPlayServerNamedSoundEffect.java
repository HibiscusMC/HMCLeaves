/*
 *
 *  *     HMCLeaves
 *  *     Copyright (C) 2022  Hibiscus Creative Studios
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU General Public License
 *  *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package io.github.fisher2911.hmcleaves.packet;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.sound.SoundCategory;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

import java.util.Optional;

public class WrapperPlayServerNamedSoundEffect extends PacketWrapper<WrapperPlayServerNamedSoundEffect> {

    private int soundID;
    private Optional<String> soundName;
    private Optional<Boolean> fixedSoundRange;
    private Optional<Float> range;
    private SoundCategory soundCategory;
    private Vector3i effectPosition;
    private float volume;
    private float pitch;
    private long seed;

    public WrapperPlayServerNamedSoundEffect(PacketSendEvent event) {
        super(event);
    }

    public WrapperPlayServerNamedSoundEffect(
            int soundID,
            Optional<String> soundName,
            Optional<Boolean> fixedSoundRange,
            Optional<Float> range,
            SoundCategory soundCategory,
            Vector3i effectPosition,
            float volume,
            float pitch
    ) {
        this(soundID, soundName, fixedSoundRange, range, soundCategory, effectPosition, volume, pitch, -1L);
    }

    public WrapperPlayServerNamedSoundEffect(
            int soundID,
            Optional<String> soundName,
            Optional<Boolean> fixedSoundRange,
            Optional<Float> range,
            SoundCategory soundCategory,
            Vector3i effectPosition,
            float volume,
            float pitch,
            long seed
    ) {
        super(PacketType.Play.Server.SOUND_EFFECT);
        this.soundID = soundID;
        this.soundName = soundName;
        this.fixedSoundRange = fixedSoundRange;
        this.range = range;
        this.soundCategory = soundCategory;
        this.effectPosition = effectPosition;
        this.volume = volume;
        this.pitch = pitch;
        this.seed = seed;
    }

    public void read() {
        this.soundID = this.readVarInt();
        if (this.soundID == 0) {
            this.soundName = Optional.of(this.readString());
            this.fixedSoundRange = Optional.of(this.readBoolean());
            if (this.fixedSoundRange.get()) {
                this.range = Optional.of(this.readFloat());
            } else {
                this.range = Optional.empty();
            }
        } else {
            this.soundName = Optional.empty();
            this.fixedSoundRange = Optional.empty();
            this.range = Optional.empty();
        }
        this.soundCategory = SoundCategory.fromId(this.readVarInt());
        this.effectPosition = new Vector3i(this.readInt(), this.readInt(), this.readInt());
        this.volume = this.readFloat();
        this.pitch = this.readFloat();
        if (this.serverVersion.isNewerThanOrEquals(ServerVersion.V_1_19)) {
            this.seed = this.readLong();
        }

    }

    public void write() {
        this.writeVarInt(this.soundID);
        if (this.soundID == 0) {
            this.writeString(this.soundName.get());
            this.writeBoolean(this.fixedSoundRange.get());
            if (this.fixedSoundRange.get()) {
                this.writeFloat(this.range.get());
            }
        }
        this.writeVarInt(this.soundCategory.ordinal());
        this.writeInt(this.effectPosition.getX() * 8);
        this.writeInt(this.effectPosition.getY() * 8);
        this.writeInt(this.effectPosition.getZ() * 8);
        this.writeFloat(this.volume);
        this.writeFloat(this.pitch);
        if (this.serverVersion.isNewerThanOrEquals(ServerVersion.V_1_19)) {
            this.writeLong(this.seed);
        }
    }

    public int getSoundID() {
        return soundID;
    }

    public void setSoundID(int soundID) {
        this.soundID = soundID;
    }

    public Optional<String> getSoundName() {
        return soundName;
    }

    public void setSoundName(String soundName) {
        this.soundName = Optional.of(soundName);
    }

    public Optional<Boolean> getFixedSoundRange() {
        return fixedSoundRange;
    }

    public void setFixedSoundRange(Boolean fixedSoundRange) {
        this.fixedSoundRange = Optional.of(fixedSoundRange);
    }

    public Optional<Float> getRange() {
        return range;
    }

    public void setRange(Float range) {
        this.range = Optional.of(range);
    }

    public SoundCategory getSoundCategory() {
        return soundCategory;
    }

    public void setSoundCategory(SoundCategory soundCategory) {
        this.soundCategory = soundCategory;
    }

    public Vector3i getEffectPosition() {
        return effectPosition;
    }

    public void setEffectPosition(Vector3i effectPosition) {
        this.effectPosition = effectPosition;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

}
