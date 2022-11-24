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
