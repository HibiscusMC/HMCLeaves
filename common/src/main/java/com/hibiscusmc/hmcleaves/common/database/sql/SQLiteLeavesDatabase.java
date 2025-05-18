package com.hibiscusmc.hmcleaves.common.database.sql;

import com.hibiscusmc.hmcleaves.common.block.LeavesBlock;
import com.hibiscusmc.hmcleaves.common.config.LeavesConfig;
import com.hibiscusmc.hmcleaves.common.database.DatabaseUtils;
import com.hibiscusmc.hmcleaves.common.database.LeavesDatabase;
import com.hibiscusmc.hmcleaves.common.util.PositionUtils;
import com.hibiscusmc.hmcleaves.common.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.common.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.common.world.LeavesChunkSection;
import com.hibiscusmc.hmcleaves.common.world.Position;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SQLiteLeavesDatabase implements LeavesDatabase {

    private static final String BLOCKS_TABLE_NAME = "blocks";
    private static final String BLOCKS_COLUMN_POSITION_WORLD_UUID = "world_uuid";
    private static final String BLOCKS_CHUNK_KEY = "chunk_key";
    private static final String BLOCKS_COLUMN_POSITION_X = "block_x";
    private static final String BLOCKS_COLUMN_POSITION_Y = "block_y";
    private static final String BLOCKS_COLUMN_POSITION_Z = "block_z";
    private static final String BLOCKS_COLUMN_BLOCK = "block";

    private static final String SCANNED_CHUNKS_TABLE_NAME = "scanned_chunks";
    private static final String SCANNED_CHUNKS_COLUMN_WORLD_UUID = "world_uuid";
    private static final String SCANNED_CHUNKS_CHUNK_KEY = "chunk_key";
    private static final String SCANNED_CHUNKS_COLUMN_VERSION = "version";

    private static final String CREATE_BLOCKS_TABLE_STATEMENT = "CREATE TABLE IF NOT EXISTS " +
            BLOCKS_TABLE_NAME + " (" +
            BLOCKS_COLUMN_POSITION_WORLD_UUID + " BINARY(16) NOT NULL, " +
            BLOCKS_CHUNK_KEY + " BIGINT NOT NULL, " +
            BLOCKS_COLUMN_POSITION_X + " INTEGER NOT NULL, " +
            BLOCKS_COLUMN_POSITION_Y + " INTEGER NOT NULL, " +
            BLOCKS_COLUMN_POSITION_Z + " INTEGER NOT NULL, " +
            BLOCKS_COLUMN_BLOCK + " TEXT NOT NULL, " +
            "PRIMARY KEY (" + BLOCKS_COLUMN_POSITION_WORLD_UUID + ", " +
            BLOCKS_COLUMN_POSITION_X + ", " +
            BLOCKS_COLUMN_POSITION_Y + ", " +
            BLOCKS_COLUMN_POSITION_Z + ")" +
            ");";

    private static final String CREATE_SCANNED_CHUNKS_TABLE_STATEMENT = "CREATE TABLE IF NOT EXISTS " +
            SCANNED_CHUNKS_TABLE_NAME + " (" +
            SCANNED_CHUNKS_COLUMN_WORLD_UUID + " BINARY(16) NOT NULL, " +
            SCANNED_CHUNKS_CHUNK_KEY + " INTEGER NOT NULL, " +
            SCANNED_CHUNKS_COLUMN_VERSION + " INTEGER NULL, " +
            "PRIMARY KEY (" + SCANNED_CHUNKS_COLUMN_WORLD_UUID + ", " +
            SCANNED_CHUNKS_CHUNK_KEY + ")" +
            ");";

    private static final String SELECT_BLOCKS_IN_CHUNK_STATEMENT = "SELECT " +
            BLOCKS_COLUMN_POSITION_X + ", " +
            BLOCKS_COLUMN_POSITION_Y + ", " +
            BLOCKS_COLUMN_POSITION_Z + ", " +
            BLOCKS_COLUMN_BLOCK +
            " FROM " +
            BLOCKS_TABLE_NAME + " WHERE " +
            BLOCKS_COLUMN_POSITION_WORLD_UUID + " = ? AND " +
            BLOCKS_CHUNK_KEY + " = ?;";

    private static final String SELECT_SCANNED_CHUNKS_STATEMENT = "SELECT " +
            SCANNED_CHUNKS_COLUMN_WORLD_UUID + ", " +
            SCANNED_CHUNKS_CHUNK_KEY + ", " +
            SCANNED_CHUNKS_COLUMN_VERSION +
            " FROM " +
            SCANNED_CHUNKS_TABLE_NAME + " WHERE " +
            SCANNED_CHUNKS_COLUMN_WORLD_UUID + " = ? AND " +
            SCANNED_CHUNKS_CHUNK_KEY + " = ?;";

    private static final String INSERT_BLOCK_STATEMENT = "INSERT INTO " +
            BLOCKS_TABLE_NAME + " (" +
            BLOCKS_COLUMN_POSITION_WORLD_UUID + ", " +
            BLOCKS_CHUNK_KEY + ", " +
            BLOCKS_COLUMN_POSITION_X + ", " +
            BLOCKS_COLUMN_POSITION_Y + ", " +
            BLOCKS_COLUMN_POSITION_Z + ", " +
            BLOCKS_COLUMN_BLOCK +
            ") VALUES (?,?,?,?,?,?);";

    private static final String INSERT_SCANNED_CHUNK_STATEMENT = "INSERT INTO " +
            SCANNED_CHUNKS_TABLE_NAME + " (" +
            SCANNED_CHUNKS_COLUMN_WORLD_UUID + ", " +
            SCANNED_CHUNKS_CHUNK_KEY + ", " +
            SCANNED_CHUNKS_COLUMN_VERSION +
            ") VALUES (?,?,?);";

    private static final String DELETE_CHUNK_BLOCKS_STATEMENT = "DELETE FROM " +
            BLOCKS_TABLE_NAME + " WHERE " +
            BLOCKS_COLUMN_POSITION_WORLD_UUID + " = ? AND " +
            BLOCKS_CHUNK_KEY + " = ?;";


    private final Path databasePath;
    private Connection connection;
    private final LeavesConfig<?> config;

    private final ExecutorService readExecutor;
    private final ExecutorService writeExecutor;

    public SQLiteLeavesDatabase(Path databasePath, LeavesConfig<?> config) {
        this.databasePath = databasePath;
        this.config = config;
        this.readExecutor = Executors.newFixedThreadPool(10);
        this.writeExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void executeRead(Runnable runnable) {
        this.readExecutor.execute(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void executeWrite(Runnable runnable) {
        this.writeExecutor.execute(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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
        try (
                final PreparedStatement blocksStatement = connection.prepareStatement(CREATE_BLOCKS_TABLE_STATEMENT);
                final PreparedStatement scanStatement = connection.prepareStatement(CREATE_SCANNED_CHUNKS_TABLE_STATEMENT);
        ) {
            blocksStatement.execute();
            scanStatement.execute();
        }
    }

    @Override
    public @Nullable LeavesChunk loadChunk(UUID worldId, ChunkPosition chunkSectionPosition) throws SQLException {
        final Connection connection = this.getConnection();
        final Map<Position, LeavesBlock> blocks = new HashMap<>();
        try (final PreparedStatement statement = connection.prepareStatement(SELECT_BLOCKS_IN_CHUNK_STATEMENT)) {
            statement.setBytes(1, DatabaseUtils.uuidToBytes(worldId));
            final long chunkKey = PositionUtils.getChunkKey(chunkSectionPosition.x(), chunkSectionPosition.z());
            statement.setLong(2, chunkKey);
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
        final LeavesChunk chunk = new LeavesChunk(chunkSectionPosition, new HashMap<>(), position -> new LeavesChunkSection(position, new ConcurrentHashMap<>()));
        for (var entry : blocks.entrySet()) {
            final Position position = entry.getKey();
            final LeavesBlock block = entry.getValue();
            chunk.setBlock(position, block);
        }
        chunk.setDirty(false);
        return chunk;
    }

    @Override
    public boolean hasChunkBeenScanned(UUID worldId, ChunkPosition position) throws SQLException {
        try (final PreparedStatement statement = this.getConnection().prepareStatement(SELECT_SCANNED_CHUNKS_STATEMENT)) {
            statement.setBytes(1, DatabaseUtils.uuidToBytes(worldId));
            final long chunkKey = PositionUtils.getChunkKey(position.x(), position.z());
            statement.setLong(2, chunkKey);
            try (final var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    final int version = resultSet.getInt(SCANNED_CHUNKS_COLUMN_VERSION);
                    return version == this.config.chunkScanVersion();
                } else {
                    return false;
                }
            }
        }
    }

    @Override
    public void setChunkScanned(UUID worldId, ChunkPosition position) throws SQLException {
        try (final PreparedStatement statement = this.getConnection().prepareStatement(INSERT_SCANNED_CHUNK_STATEMENT)) {
            statement.setBytes(1, DatabaseUtils.uuidToBytes(worldId));
            final long chunkKey = PositionUtils.getChunkKey(position.x(), position.z());
            statement.setLong(2, chunkKey);
            statement.setInt(3, this.config.chunkScanVersion());
            statement.executeUpdate();
        }
    }

    public void saveChunk(LeavesChunk chunk) throws SQLException {
        final Connection connection = this.getConnection();
        connection.setAutoCommit(false);
        try (
                final PreparedStatement deleteStatement = connection.prepareStatement(DELETE_CHUNK_BLOCKS_STATEMENT);
                final PreparedStatement insertStatement = connection.prepareStatement(INSERT_BLOCK_STATEMENT);
        ) {
            deleteStatement.setBytes(1, DatabaseUtils.uuidToBytes(chunk.chunkPosition().worldId()));
            final long chunkKey = PositionUtils.getChunkKey(chunk.chunkPosition().x(), chunk.chunkPosition().z());
            deleteStatement.setLong(2, chunkKey);
            deleteStatement.executeUpdate();

            for (LeavesChunkSection section : chunk.getSections().values()) {
                for (var entry : section.getBlocks().entrySet()) {
                    final Position position = entry.getKey();
                    final LeavesBlock block = entry.getValue();
                    insertStatement.setBytes(1, DatabaseUtils.uuidToBytes(position.worldId()));
                    insertStatement.setLong(2, chunkKey);
                    insertStatement.setInt(3, position.x());
                    insertStatement.setInt(4, position.y());
                    insertStatement.setInt(5, position.z());
                    insertStatement.setString(6, block.id());
                    insertStatement.addBatch();
                }
            }
            insertStatement.executeBatch();

            connection.commit();
        }
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
