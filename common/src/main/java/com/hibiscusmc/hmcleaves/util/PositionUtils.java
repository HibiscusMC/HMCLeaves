package com.hibiscusmc.hmcleaves.util;

public final class PositionUtils {

    private PositionUtils() {

    }

    public static int coordToChunkCoord(int coord) {
        return coord >> 4;
    }

    public static int chunkCoordToCoord(int chunkCoord, int coordInChunk) {
        return (chunkCoord << 4) + coordInChunk;
    }

    public static int coordToCoordInChunk(int coord) {
        return coord & 15;
    }

}
