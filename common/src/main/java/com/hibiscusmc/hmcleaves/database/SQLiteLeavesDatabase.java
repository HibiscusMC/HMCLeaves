package com.hibiscusmc.hmcleaves.database;

import com.hibiscusmc.hmcleaves.block.LeavesBlock;
import com.hibiscusmc.hmcleaves.config.LeavesConfig;
import com.hibiscusmc.hmcleaves.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.world.Position;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SQLiteLeavesDatabase implements LeavesDatabase {

    private static final String BLOCKS_TABLE_NAME = "blocks";
    private static final String BLOCKS_COLUMN_POSITION_WORLD_UUID = "world_uuid";
    private static final String BLOCKS_CHUNK_KEY = "chunk_key";
    private static final String BLOCKS_CHUNK_Y_COLUMN = "chunk_y";
    private static final String BLOCKS_COLUMN_POSITION_X = "block_x";
    private static final String BLOCKS_COLUMN_POSITION_Y = "block_y";
    private static final String BLOCKS_COLUMN_POSITION_Z = "block_z";
    private static final String BLOCKS_COLUMN_BLOCK = "block";
    private static final String CREATE_BLOCKS_TABLE_STATEMENT = "CREATE TABLE IF NOT EXISTS " +
            BLOCKS_TABLE_NAME + " (" +
            BLOCKS_COLUMN_POSITION_WORLD_UUID + " BINARY(16) NOT NULL, " +
            BLOCKS_CHUNK_KEY + " INTEGER NOT NULL, " +
            BLOCKS_CHUNK_Y_COLUMN + " INTEGER NOT NULL, " +
            BLOCKS_COLUMN_POSITION_X + " INTEGER NOT NULL, " +
            BLOCKS_COLUMN_POSITION_Y + " INTEGER NOT NULL, " +
            BLOCKS_COLUMN_POSITION_Z + " INTEGER NOT NULL, " +
            BLOCKS_COLUMN_BLOCK + " TEXT NOT NULL, " +
            "PRIMARY KEY (" + BLOCKS_COLUMN_POSITION_WORLD_UUID + ", " +
            BLOCKS_COLUMN_POSITION_X + ", " +
            BLOCKS_COLUMN_POSITION_Y + ", " +
            BLOCKS_COLUMN_POSITION_Z + ")" +
            ");";

    private static final String SELECT_BLOCKS_IN_CHUNK_STATEMENT = "SELECT " +
            BLOCKS_COLUMN_POSITION_X + ", " +
            BLOCKS_COLUMN_POSITION_Y + ", " +
            BLOCKS_COLUMN_POSITION_Z + ", " +
            BLOCKS_COLUMN_BLOCK +
            " FROM " +
            BLOCKS_TABLE_NAME + " WHERE " +
            BLOCKS_COLUMN_POSITION_WORLD_UUID + " = ? AND " +
            BLOCKS_CHUNK_KEY + " = ? AND " +
            BLOCKS_CHUNK_Y_COLUMN + " = ?;";

    private static final String INSERT_BLOCK_STATEMENT = "INSERT INTO " +
            BLOCKS_TABLE_NAME + " (" +
            BLOCKS_COLUMN_POSITION_WORLD_UUID + ", " +
            BLOCKS_CHUNK_KEY + ", " +
            BLOCKS_CHUNK_Y_COLUMN + ", " +
            BLOCKS_COLUMN_POSITION_X + ", " +
            BLOCKS_COLUMN_POSITION_Y + ", " +
            BLOCKS_COLUMN_POSITION_Z + ", " +
            BLOCKS_COLUMN_BLOCK +
            ") VALUES (?, ?, ?, ?, ?, ?, ?);";


    private final Path databasePath;
    private Connection connection;
    private final LeavesConfig config;

    public SQLiteLeavesDatabase(Path databasePath, LeavesConfig config) {
        this.databasePath = databasePath;
        this.config = config;
    }

    @Override
    public boolean init() {
        try {
            this.createTables();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void createTables() throws SQLException {
        final Connection connection = this.getConnection();
        try (final PreparedStatement statement = connection.prepareStatement(CREATE_BLOCKS_TABLE_STATEMENT)) {
            statement.execute();
        }
    }

    @Override
    public @Nullable LeavesChunk loadChunk(UUID worldId, ChunkPosition chunkPosition) throws SQLException {
        final Connection connection = this.getConnection();
        final Map<Position, LeavesBlock> blocks = new HashMap<>();
        try (final PreparedStatement statement = connection.prepareStatement(SELECT_BLOCKS_IN_CHUNK_STATEMENT)) {
            statement.setBytes(1, DatabaseUtils.uuidToBytes(worldId));
            statement.setInt(2, chunkPosition.x());
            statement.setInt(3, chunkPosition.y());
            try (final var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    final int x = resultSet.getInt(BLOCKS_COLUMN_POSITION_X);
                    final int y = resultSet.getInt(BLOCKS_COLUMN_POSITION_Y);
                    final int z = resultSet.getInt(BLOCKS_COLUMN_POSITION_Z);
                    final String block = resultSet.getString(BLOCKS_COLUMN_BLOCK);
                    blocks.put(new Position(worldId, x, y, z), this.config.getBlock(block));
                }
            }
        }
        if (blocks.isEmpty()) {
            return null;
        }
        return new LeavesChunk(chunkPosition, blocks);
    }


    @NotNull
    private Connection getConnection() throws IllegalStateException {
        if (this.connection != null) return this.connection;
        final File folder = this.databasePath.getParent().toFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.databasePath);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not connect to database!", e);
        }
        return this.connection;
    }


}
