package com.hibiscusmc.hmcleaves.paper.util;

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

    public static long getChunkKey(long x, long z) {
        return (x & 0xFFFFFFFFL) | ((z & 0xFFFFFFFFL) << 32);
    }

}
