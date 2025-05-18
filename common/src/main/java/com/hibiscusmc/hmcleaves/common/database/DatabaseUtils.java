package com.hibiscusmc.hmcleaves.common.database;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public final class DatabaseUtils {

    private DatabaseUtils() {
    }

    public static byte[] uuidToBytes(UUID uuid) {
        return ByteBuffer.wrap(new byte[16])
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits()).array();
    }

    public static UUID bytesToUUID(byte[] bytes) {
        final ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        final long firstLong = bb.getLong();
        final long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }

}
