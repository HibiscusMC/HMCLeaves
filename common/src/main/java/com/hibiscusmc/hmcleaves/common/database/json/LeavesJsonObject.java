package com.hibiscusmc.hmcleaves.common.database.json;

import com.hibiscusmc.hmcleaves.common.world.Position;

public final class LeavesJsonObject {

    private final String id;
    private final int x;
    private final int y;
    private final int z;

    public LeavesJsonObject(String id, int x, int y, int z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public LeavesJsonObject(String id, Position position) {
        this(id, position.x(), position.y(), position.z());
    }

    public String id() {
        return this.id;
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public int z() {
        return this.z;
    }

}
