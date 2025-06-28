package com.hibiscusmc.hmcleaves.paper.util;

public final class MathUtils {

    private MathUtils() {
    }

    public static long distanceSquared(int x1, int y1, int x2, int y2) {
        return (long) (Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

}
