package io.github.fisher2911.hmcleaves.data;

import com.github.retrooper.packetevents.protocol.sound.SoundCategory;

public record SoundData(String name, SoundCategory soundCategory, float volume, float pitch) {

}
