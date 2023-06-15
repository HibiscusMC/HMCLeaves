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

package io.github.fisher2911.hmcleaves.data;

import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.cache.ChunkBlockCache;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.world.ChunkPosition;
import io.github.fisher2911.hmcleaves.world.Position;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LeafDatabase {

    private final HMCLeaves plugin;
    private final Path databaseFilePath;
    private final Executor executor;
    private Connection connection;

    public LeafDatabase(HMCLeaves plugin) {
        this.plugin = plugin;
        this.databaseFilePath = this.plugin.getDataFolder().toPath().resolve("database").resolve("leaves.db");
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Nullable
    private Connection getConnection() {
        if (this.connection != null) return this.connection;
        final File folder = this.databaseFilePath.getParent().toFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.databaseFilePath.toString());
        } catch (SQLException e) {
            throw new IllegalStateException("Could not connect to database!", e);
        }
        return this.connection;
    }

    public void load() {
        this.createTables();
    }

    public void doDatabaseTaskAsync(Runnable runnable) {
        this.executor.execute(runnable);
    }

    private static final String LEAVES_TABLE_NAME = "leaves";
    private static final String LEAVES_TABLE_WORLD_UUID_COLUMN = "world_uuid";
    private static final String LEAVES_TABLE_CHUNK_X_COLUMN = "chunk_x";
    private static final String LEAVES_TABLE_CHUNK_Z_COLUMN = "chunk_z";
    private static final String LEAVES_TABLE_BLOCK_X_COLUMN = "block_x";
    private static final String LEAVES_TABLE_BLOCK_Y_COLUMN = "block_y";
    private static final String LEAVES_TABLE_BLOCK_Z_COLUMN = "block_z";
    private static final String LEAVES_TABLE_BLOCK_ID_COLUMN = "block_id";
    private static final String LEAVES_TABLE_WATERLOGGED_COLUMN = "waterlogged";
    private static final String CREATE_LEAVES_TABLE_STATEMENT =
            "CREATE TABLE IF NOT EXISTS " + LEAVES_TABLE_NAME + " (" +
                    LEAVES_TABLE_WORLD_UUID_COLUMN + " BINARY(16) NOT NULL, " +
                    LEAVES_TABLE_CHUNK_X_COLUMN + " INTEGER NOT NULL, " +
                    LEAVES_TABLE_CHUNK_Z_COLUMN + " INTEGER NOT NULL, " +
                    LEAVES_TABLE_BLOCK_X_COLUMN + " INTEGER NOT NULL, " +
                    LEAVES_TABLE_BLOCK_Y_COLUMN + " INTEGER NOT NULL, " +
                    LEAVES_TABLE_BLOCK_Z_COLUMN + " INTEGER NOT NULL, " +
                    LEAVES_TABLE_BLOCK_ID_COLUMN + " TEXT NOT NULL, " +
                    LEAVES_TABLE_WATERLOGGED_COLUMN + " BOOLEAN NOT NULL, " +
                    "PRIMARY KEY (" + LEAVES_TABLE_WORLD_UUID_COLUMN + ", " + LEAVES_TABLE_BLOCK_X_COLUMN + ", " + LEAVES_TABLE_BLOCK_Y_COLUMN + ", " + LEAVES_TABLE_BLOCK_Z_COLUMN + ")" +
                    ");";

    private static final String SET_LEAF_BLOCK_STATEMENT =
            "INSERT OR REPLACE INTO " + LEAVES_TABLE_NAME + " (" +
                    LEAVES_TABLE_WORLD_UUID_COLUMN + ", " +
                    LEAVES_TABLE_CHUNK_X_COLUMN + ", " +
                    LEAVES_TABLE_CHUNK_Z_COLUMN + ", " +
                    LEAVES_TABLE_BLOCK_X_COLUMN + ", " +
                    LEAVES_TABLE_BLOCK_Y_COLUMN + ", " +
                    LEAVES_TABLE_BLOCK_Z_COLUMN + ", " +
                    LEAVES_TABLE_BLOCK_ID_COLUMN + ", " +
                    LEAVES_TABLE_WATERLOGGED_COLUMN +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?);";

    private static final String GET_LEAF_BLOCK_STATEMENT =
            "SELECT " + LEAVES_TABLE_BLOCK_ID_COLUMN + ", " + LEAVES_TABLE_WATERLOGGED_COLUMN + " FROM " + LEAVES_TABLE_NAME + " WHERE " +
                    LEAVES_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    LEAVES_TABLE_BLOCK_X_COLUMN + " = ? AND " +
                    LEAVES_TABLE_BLOCK_Y_COLUMN + " = ? AND " +
                    LEAVES_TABLE_BLOCK_Z_COLUMN + " = ?;";

    private static final String GET_LEAF_BLOCKS_IN_CHUNK_STATEMENT =
            "SELECT " + LEAVES_TABLE_BLOCK_X_COLUMN + ", " + LEAVES_TABLE_BLOCK_Y_COLUMN + ", " + LEAVES_TABLE_BLOCK_Z_COLUMN + ", " + LEAVES_TABLE_BLOCK_ID_COLUMN + ", " + LEAVES_TABLE_WATERLOGGED_COLUMN + " FROM " + LEAVES_TABLE_NAME + " WHERE " +
                    LEAVES_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    LEAVES_TABLE_CHUNK_X_COLUMN + " = ? AND " +
                    LEAVES_TABLE_CHUNK_Z_COLUMN + " = ?;";

    private static final String DELETE_LEAF_BLOCK_STATEMENT =
            "DELETE FROM " + LEAVES_TABLE_NAME + " WHERE " +
                    LEAVES_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    LEAVES_TABLE_BLOCK_X_COLUMN + " = ? AND " +
                    LEAVES_TABLE_BLOCK_Y_COLUMN + " = ? AND " +
                    LEAVES_TABLE_BLOCK_Z_COLUMN + " = ?;";

//    private static final String DELETE_LEAF_BLOCKS_IN_CHUNK_STATEMENT =
//            "DELETE FROM " + LEAVES_TABLE_NAME + " WHERE " +
//                    LEAVES_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
//                    LEAVES_TABLE_CHUNK_X_COLUMN + " = ? AND " +
//                    LEAVES_TABLE_CHUNK_Z_COLUMN + " = ?;";

    private static final String LOGS_TABLE_NAME = "logs";
    private static final String LOGS_TABLE_WORLD_UUID_COLUMN = "world_uuid";
    private static final String LOGS_TABLE_CHUNK_X_COLUMN = "chunk_x";
    private static final String LOGS_TABLE_CHUNK_Z_COLUMN = "chunk_z";
    private static final String LOGS_TABLE_BLOCK_X_COLUMN = "block_x";
    private static final String LOGS_TABLE_BLOCK_Y_COLUMN = "block_y";
    private static final String LOGS_TABLE_BLOCK_Z_COLUMN = "block_z";
    private static final String LOGS_TABLE_BLOCK_ID_COLUMN = "block_id";
    private static final String LOGS_TABLE_STRIPPED_COLUMN = "stripped";
    private static final String CREATE_LOGS_TABLE_STATEMENT =
            "CREATE TABLE IF NOT EXISTS " + LOGS_TABLE_NAME + " (" +
                    LOGS_TABLE_WORLD_UUID_COLUMN + " BINARY(16) NOT NULL, " +
                    LOGS_TABLE_CHUNK_X_COLUMN + " INTEGER NOT NULL, " +
                    LOGS_TABLE_CHUNK_Z_COLUMN + " INTEGER NOT NULL, " +
                    LOGS_TABLE_BLOCK_X_COLUMN + " INTEGER NOT NULL, " +
                    LOGS_TABLE_BLOCK_Y_COLUMN + " INTEGER NOT NULL, " +
                    LOGS_TABLE_BLOCK_Z_COLUMN + " INTEGER NOT NULL, " +
                    LOGS_TABLE_BLOCK_ID_COLUMN + " TEXT NOT NULL, " +
                    LOGS_TABLE_STRIPPED_COLUMN + " BOOLEAN NOT NULL, " +
                    "PRIMARY KEY (" + LOGS_TABLE_WORLD_UUID_COLUMN + ", " + LOGS_TABLE_BLOCK_X_COLUMN + ", " + LOGS_TABLE_BLOCK_Y_COLUMN + ", " + LOGS_TABLE_BLOCK_Z_COLUMN + ")" +
                    ");";

    private static final String SET_LOG_BLOCK_STATEMENT =
            "INSERT OR REPLACE INTO " + LOGS_TABLE_NAME + " (" +
                    LOGS_TABLE_WORLD_UUID_COLUMN + ", " +
                    LOGS_TABLE_CHUNK_X_COLUMN + ", " +
                    LOGS_TABLE_CHUNK_Z_COLUMN + ", " +
                    LOGS_TABLE_BLOCK_X_COLUMN + ", " +
                    LOGS_TABLE_BLOCK_Y_COLUMN + ", " +
                    LOGS_TABLE_BLOCK_Z_COLUMN + ", " +
                    LOGS_TABLE_BLOCK_ID_COLUMN + ", " +
                    LOGS_TABLE_STRIPPED_COLUMN +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?);";

    private static final String GET_LOG_BLOCK_STATEMENT =
            "SELECT " + LOGS_TABLE_BLOCK_ID_COLUMN + ", " + LOGS_TABLE_STRIPPED_COLUMN + " FROM " + LOGS_TABLE_NAME + " WHERE " +
                    LOGS_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    LOGS_TABLE_BLOCK_X_COLUMN + " = ? AND " +
                    LOGS_TABLE_BLOCK_Y_COLUMN + " = ? AND " +
                    LOGS_TABLE_BLOCK_Z_COLUMN + " = ?;";

    private static final String GET_LOG_BLOCKS_IN_CHUNK_STATEMENT =
            "SELECT " + LOGS_TABLE_BLOCK_X_COLUMN + ", " + LOGS_TABLE_BLOCK_Y_COLUMN + ", " + LOGS_TABLE_BLOCK_Z_COLUMN + ", " + LOGS_TABLE_BLOCK_ID_COLUMN + ", " + LOGS_TABLE_STRIPPED_COLUMN + " FROM " + LOGS_TABLE_NAME + " WHERE " +
                    LOGS_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    LOGS_TABLE_CHUNK_X_COLUMN + " = ? AND " +
                    LOGS_TABLE_CHUNK_Z_COLUMN + " = ?;";

    private static final String DELETE_LOG_BLOCK_STATEMENT =
            "DELETE FROM " + LOGS_TABLE_NAME + " WHERE " +
                    LOGS_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    LOGS_TABLE_BLOCK_X_COLUMN + " = ? AND " +
                    LOGS_TABLE_BLOCK_Y_COLUMN + " = ? AND " +
                    LOGS_TABLE_BLOCK_Z_COLUMN + " = ?;";

//    private static final String DELETE_LOG_BLOCKS_IN_CHUNK_STATEMENT =
//            "DELETE FROM " + LOGS_TABLE_NAME + " WHERE " +
//                    LOGS_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
//                    LOGS_TABLE_CHUNK_X_COLUMN + " = ? AND " +
//                    LOGS_TABLE_CHUNK_Z_COLUMN + " = ?;";

    private void createTables() {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        try (final PreparedStatement statement = connection.prepareStatement(CREATE_LEAVES_TABLE_STATEMENT)) {
            statement.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create tables!", e);
        }
        try (final PreparedStatement statement = connection.prepareStatement(CREATE_LOGS_TABLE_STATEMENT)) {
            statement.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create tables!", e);
        }
    }

    public void close() {
        try {
            this.connection.close();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not close database connection!", e);
        }
    }

    public void setLeafBlock(UUID worldUUID, int chunkX, int chunkZ, int blockX, int blockY, int blockZ, int blockID) {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        try (final PreparedStatement statement = connection.prepareStatement(SET_LEAF_BLOCK_STATEMENT)) {
            statement.setBytes(1, this.uuidToBytes(worldUUID));
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            statement.setInt(4, blockX);
            statement.setInt(5, blockY);
            statement.setInt(6, blockZ);
            statement.setInt(7, blockID);
            statement.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not set leaf block at position " +
                    blockX + ", " + blockY + ", " + blockZ + "!", e);
        }
    }

    public void saveBlocksInChunk(ChunkBlockCache chunk) {
        chunk.setSaving(true);
        this.saveLeafBlocksInChunk(chunk);
        this.saveLogBlocksInChunk(chunk);
        chunk.setSaving(false);
        chunk.markClean();
        chunk.setSafeToMarkClean(true);
//        System.out.println("Saving chunk: " + chunk.getChunkPosition());
    }

    private void saveLeafBlocksInChunk(ChunkBlockCache chunk) {
        this.deleteRemovedLeafBlocksInChunk(chunk);
        if (chunk.getBlockDataMap().isEmpty()) return;
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final ChunkPosition chunkPosition = chunk.getChunkPosition();
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        try (final PreparedStatement statement = connection.prepareStatement(SET_LEAF_BLOCK_STATEMENT)) {
            for (var entry : chunk.getBlockDataMap().entrySet()) {
                final Position position = entry.getKey();
                final int blockX = position.x();
                final int blockY = position.y();
                final int blockZ = position.z();
                final BlockData blockData = entry.getValue();
                if (!(blockData instanceof final LeafData leafData)) continue;
                final String blockID = blockData.id();
                statement.setBytes(1, worldUUIDBytes);
                statement.setInt(2, chunkX);
                statement.setInt(3, chunkZ);
                statement.setInt(4, blockX);
                statement.setInt(5, blockY);
                statement.setInt(6, blockZ);
                statement.setString(7, blockID);
                statement.setBoolean(8, leafData.waterlogged());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save leaf blocks in chunk " + chunkX + ", " + chunkZ + "!", e);
        }
    }

    public void deleteLeafBlock(UUID worldUUID, int blockX, int blockY, int blockZ) {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        try (final PreparedStatement statement = connection.prepareStatement(DELETE_LEAF_BLOCK_STATEMENT)) {
            statement.setBytes(1, this.uuidToBytes(worldUUID));
            statement.setInt(2, blockX);
            statement.setInt(3, blockY);
            statement.setInt(4, blockZ);
            statement.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete leaf block!", e);
        }
    }

    public void deleteRemovedLeafBlocksInChunk(ChunkBlockCache chunkBlockCache) {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final ChunkPosition chunkPosition = chunkBlockCache.getChunkPosition();
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        try (final PreparedStatement statement = connection.prepareStatement(DELETE_LEAF_BLOCK_STATEMENT)) {
            chunkBlockCache.clearRemovedPositions(entry -> {
                try {
                    final Position position = entry.getKey();
                    final BlockData blockData = entry.getValue();
                    if (!(blockData instanceof LeafData)) return false;
                    final int blockX = position.x();
                    final int blockY = position.y();
                    final int blockZ = position.z();
                    statement.setBytes(1, worldUUIDBytes);
                    statement.setInt(2, blockX);
                    statement.setInt(3, blockY);
                    statement.setInt(4, blockZ);
                    statement.addBatch();
                    return true;
                } catch (SQLException e) {
                    throw new IllegalStateException("Could not delete removed leaf blocks in chunk " + chunkX + ", " + chunkZ + "!", e);
                }
            });
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete removed leaf blocks in chunk " + chunkX + ", " + chunkZ + "!", e);
        }
    }

    public Map<Position, BlockData> getBlocksInChunk(ChunkPosition chunkPosition, LeavesConfig config) {
        final Map<Position, BlockData> blocks = new HashMap<>();
        blocks.putAll(this.getLeafBlocksInChunk(chunkPosition, config));
        blocks.putAll(this.getLogBlocksInChunk(chunkPosition, config));
        return blocks;
    }

    private Map<Position, BlockData> getLeafBlocksInChunk(ChunkPosition chunkPosition, LeavesConfig config) {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        final Map<Position, BlockData> leafBlocks = new HashMap<>();
        try (final PreparedStatement statement = connection.prepareStatement(GET_LEAF_BLOCKS_IN_CHUNK_STATEMENT)) {
            statement.setBytes(1, worldUUIDBytes);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    final int blockX = resultSet.getInt(LEAVES_TABLE_BLOCK_X_COLUMN);
                    final int blockY = resultSet.getInt(LEAVES_TABLE_BLOCK_Y_COLUMN);
                    final int blockZ = resultSet.getInt(LEAVES_TABLE_BLOCK_Z_COLUMN);
                    final String blockID = resultSet.getString(LEAVES_TABLE_BLOCK_ID_COLUMN);
                    final boolean waterlogged = resultSet.getBoolean(LEAVES_TABLE_WATERLOGGED_COLUMN);
                    final Position position = new Position(chunkPosition.world(), blockX, blockY, blockZ);
                    final BlockData blockData = config.getBlockData(blockID);
                    if (!(blockData instanceof final LeafData leafData)) {
                        this.plugin.getLogger().warning("Could not find block data for block ID " + blockID + " at position " +
                                blockX + ", " + blockY + ", " + blockZ + "!");
                        continue;
                    }
                    if (waterlogged) {
                        leafBlocks.put(position, leafData.waterlog(true));
                        continue;
                    }
                    leafBlocks.put(position, leafData);
                }
            }
            return leafBlocks;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not get leaf blocks in chunk " + chunkX + ", " + chunkZ + "!", e);
        }
    }

//    public void saveLogBlock(UUID worldUUID, int chunkX, int chunkZ, int blockX, int blockY, int blockZ, String blockID, boolean stripped) {
//        final Connection connection = this.getConnection();
//        if (connection == null) throw new IllegalStateException("Could not connect to database!");
//        try (final PreparedStatement statement = connection.prepareStatement(SET_LOG_BLOCK_STATEMENT)) {
//            statement.setBytes(1, this.uuidToBytes(worldUUID));
//            statement.setInt(2, chunkX);
//            statement.setInt(3, chunkZ);
//            statement.setInt(4, blockX);
//            statement.setInt(5, blockY);
//            statement.setInt(6, blockZ);
//            statement.setString(7, blockID);
//            statement.setBoolean(8, stripped);
//            statement.execute();
//        } catch (SQLException e) {
//            throw new IllegalStateException("Could not set log block at position "
//                    + blockX + ", " + blockY + ", " + blockZ + "!", e);
//        }
//    }

    private void saveLogBlocksInChunk(ChunkBlockCache chunkBlockCache) {
        this.deleteRemovedLogBlocksInChunk(chunkBlockCache);
        if (chunkBlockCache.getBlockDataMap().isEmpty()) return;
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final ChunkPosition chunkPosition = chunkBlockCache.getChunkPosition();
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        try (final PreparedStatement statement = connection.prepareStatement(SET_LOG_BLOCK_STATEMENT)) {
            for (var entry : chunkBlockCache.getBlockDataMap().entrySet()) {
                final Position position = entry.getKey();
                final BlockData blockData = entry.getValue();
                if (!(blockData instanceof final LogData logData)) continue;
                final int blockX = position.x();
                final int blockY = position.y();
                final int blockZ = position.z();
                statement.setBytes(1, worldUUIDBytes);
                statement.setInt(2, chunkX);
                statement.setInt(3, chunkZ);
                statement.setInt(4, blockX);
                statement.setInt(5, blockY);
                statement.setInt(6, blockZ);
                statement.setString(7, blockData.id());
                statement.setBoolean(8, logData.stripped());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save log blocks in chunk " + chunkX + ", " + chunkZ + "!", e);
        }
    }

    public void deleteLogBlock(UUID worldUUID, int blockX, int blockY, int blockZ) {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        try (final PreparedStatement statement = connection.prepareStatement(DELETE_LOG_BLOCK_STATEMENT)) {
            statement.setBytes(1, this.uuidToBytes(worldUUID));
            statement.setInt(2, blockX);
            statement.setInt(3, blockY);
            statement.setInt(4, blockZ);
            statement.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete log block at position "
                    + blockX + ", " + blockY + ", " + blockZ + "!", e);
        }
    }

    public void deleteRemovedLogBlocksInChunk(ChunkBlockCache chunkBlockCache) {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final ChunkPosition chunkPosition = chunkBlockCache.getChunkPosition();
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        try (final PreparedStatement statement = connection.prepareStatement(DELETE_LOG_BLOCK_STATEMENT)) {
            chunkBlockCache.clearRemovedPositions(entry -> {
                try {
                    final Position position = entry.getKey();
                    final BlockData blockData = entry.getValue();
                    if (!(blockData instanceof LogData)) return false;
                    final int blockX = position.x();
                    final int blockY = position.y();
                    final int blockZ = position.z();
                    statement.setBytes(1, worldUUIDBytes);
                    statement.setInt(2, blockX);
                    statement.setInt(3, blockY);
                    statement.setInt(4, blockZ);
                    statement.addBatch();
                    return true;
                } catch (SQLException e) {
                    throw new IllegalStateException("Could not delete removed log blocks in chunk " + chunkX + ", " + chunkZ + "!", e);
                }
            });
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete removed log blocks in chunk " + chunkX + ", " + chunkZ + "!", e);
        }
    }

    private Map<Position, BlockData> getLogBlocksInChunk(ChunkPosition chunkPosition, LeavesConfig config) {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        final Map<Position, BlockData> logBlocks = new HashMap<>();
        try (final PreparedStatement statement = connection.prepareStatement(GET_LOG_BLOCKS_IN_CHUNK_STATEMENT)) {
            statement.setBytes(1, worldUUIDBytes);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    final int blockX = resultSet.getInt(LOGS_TABLE_BLOCK_X_COLUMN);
                    final int blockY = resultSet.getInt(LOGS_TABLE_BLOCK_Y_COLUMN);
                    final int blockZ = resultSet.getInt(LOGS_TABLE_BLOCK_Z_COLUMN);
                    final String blockID = resultSet.getString(LOGS_TABLE_BLOCK_ID_COLUMN);
                    final boolean stripped = resultSet.getBoolean(LOGS_TABLE_STRIPPED_COLUMN);
                    final Position position = new Position(chunkPosition.world(), blockX, blockY, blockZ);
                    final BlockData blockData = config.getBlockData(blockID);
                    if (!(blockData instanceof LogData logData)) {
                        this.plugin.getLogger().warning("Could not find block data for block ID " + blockID + " at position " +
                                blockX + ", " + blockY + ", " + blockZ + "!");
                        continue;
                    }
                    if (stripped) {
                        logData = logData.strip();
                    }
                    logBlocks.put(position, logData);
                }
            }
            return logBlocks;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not get log blocks in chunk " + chunkX + ", " + chunkZ + "!", e);
        }
    }

    private byte[] uuidToBytes(UUID uuid) {
        return ByteBuffer.wrap(new byte[16])
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits()).array();
    }

    private UUID bytesToUUID(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }

}
