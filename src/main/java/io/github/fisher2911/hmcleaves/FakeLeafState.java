package io.github.fisher2911.hmcleaves;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

public class FakeLeafState {

    private final WrappedBlockState state;
    private final boolean actuallyPersistent;
    private int actualDistance;

    public FakeLeafState(WrappedBlockState state, boolean actuallyPersistent, int actualDistance) {
        this.state = state;
        this.actuallyPersistent = actuallyPersistent;
        this.actualDistance = actualDistance;
    }

    public WrappedBlockState state() {
        return this.state;
    }

    public boolean actuallyPersistent() {
        return this.actuallyPersistent;
    }

    public int actualDistance() {
        return this.actualDistance;
    }

    public void actualDistance(int actualDistance) {
        this.actualDistance = Math.min(actualDistance, 7);
    }

}
