package com.hibiscusmc.hmcleaves.block;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

import java.util.function.Supplier;

public record LeavesBlock(String id, BlockProperties properties, Supplier<WrappedBlockState> blockState) {

}
