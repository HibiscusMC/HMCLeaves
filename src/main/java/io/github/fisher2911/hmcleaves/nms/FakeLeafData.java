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

package io.github.fisher2911.hmcleaves.nms;

public class FakeLeafData {

    // what the player should see
    private final int fakeDistance;
    // what the player should see
    private final boolean fakePersistence;
    // what it should be in vanilla minecraft
    private int actualDistance;
    // what it should be in vanilla minecraft
    private boolean actualPersistence;

    /**
     * @param fakeDistance      what the player should see
     * @param fakePersistence   what the player should see
     * @param actualDistance    what it should be in vanilla minecraft
     * @param actualPersistence what it should be in vanilla minecraft
     */
    public FakeLeafData(int fakeDistance, boolean fakePersistence, int actualDistance, boolean actualPersistence) {
        this(fakeDistance, fakePersistence);
        this.actualDistance = actualDistance;
        this.actualPersistence = actualPersistence;
    }

    public FakeLeafData(int fakeDistance, boolean fakePersistence) {
        this.fakeDistance = fakeDistance;
        this.fakePersistence = fakePersistence;
    }

    public int fakeDistance() {
        return this.fakeDistance;
    }

    public boolean fakePersistence() {
        return this.fakePersistence;
    }

    public int actualDistance() {
        return this.actualDistance;
    }

    public void actualDistance(int actualDistance) {
        this.actualDistance = actualDistance;
    }

    public boolean actualPersistence() {
        return this.actualPersistence;
    }

    @Override
    public String toString() {
        return "FakeLeafData{" +
                "fakeDistance=" + fakeDistance +
                ", fakePersistence=" + fakePersistence +
                ", actualDistance=" + actualDistance +
                ", actualPersistence=" + actualPersistence +
                '}';
    }

}
