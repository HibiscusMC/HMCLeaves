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

}
