package io.github.fisher2911.hmcleaves;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

public class FakeLeafState {

    public static FakeLeafState NULL = new FakeLeafState(null, false, 0);

    private final WrappedBlockState state;
    private boolean actuallyPersistent;
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

    public void actuallyPersistent(boolean actuallyPersistent) {
        this.actuallyPersistent = actuallyPersistent;
    }

    public int actualDistance() {
        return this.actualDistance;
    }

    public void actualDistance(int actualDistance) {
        this.actualDistance = Math.min(actualDistance, 7);
    }

    public FakeLeafState snapshot() {
        return new FakeLeafState(this.state.clone(), this.actuallyPersistent, this.actualDistance);
    }

    @Override
    public String toString() {
        return "FakeLeafState{" +
                "persistent=" + state.isPersistent() +
                ", distance=" + state.getDistance() +
                ", actuallyPersistent=" + actuallyPersistent +
                ", actualDistance=" + actualDistance +
                '}';
    }

}
