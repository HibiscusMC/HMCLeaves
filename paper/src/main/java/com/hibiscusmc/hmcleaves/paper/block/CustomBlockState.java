package com.hibiscusmc.hmcleaves.paper.block;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

import java.util.Map;

public record CustomBlockState(CustomBlock customBlock, int globalStateId) {

    public WrappedBlockState getBlockState() {
        return WrappedBlockState.getByGlobalId(this.globalStateId);
    }

    public Map<String, String> getPropertiesByName() {
        return this.customBlock.getPropertiesByName(
                WrappedBlockState.getByGlobalId(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(), this.globalStateId, false)
        );
    }

}
