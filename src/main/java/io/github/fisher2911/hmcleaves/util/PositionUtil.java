package io.github.fisher2911.hmcleaves.util;

public class PositionUtil {

    public static int getCoordInChunk(int i) {
        return i & 15;
    }
}
