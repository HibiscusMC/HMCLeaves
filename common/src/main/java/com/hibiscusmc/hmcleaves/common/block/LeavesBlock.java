package com.hibiscusmc.hmcleaves.common.block;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

public record LeavesBlock(String id, BlockType type, BlockProperties visualProperties, int displayBlockGlobalId) {

    public WrappedBlockState getBlockState() {
        return WrappedBlockState.getByGlobalId(this.displayBlockGlobalId);
    }

}
