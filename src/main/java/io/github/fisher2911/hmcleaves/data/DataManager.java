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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.fisher2911.hmcleaves.FakeLeafState;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class DataManager {

    private final HMCLeaves plugin;
    private final Path databasePath;
    private HikariDataSource dataSource;

    public DataManager(HMCLeaves plugin) {
        this.plugin = plugin;
        this.databasePath = this.plugin.getDataFolder().toPath().resolve("database").resolve("leaves.db");
    }

    public void load() {
        final File folder = this.databasePath.getParent().toFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + this.databasePath);
        this.dataSource = new HikariDataSource(config);
    }

    @Nullable
    public Connection getConnection() throws SQLException {
        if (this.dataSource == null) throw new IllegalStateException("Datasource not loaded!");
        return this.dataSource.getConnection();
    }

    private static final String LEAVES_TABLE_NAME = "leaves";
    private static final String LEAVES_TABLE_WORLD_UUID_COL = "world_uuid";
    private static final String LEAVES_TABLE_CHUNK_X_COL = "chunk_x";
    private static final String LEAVES_TABLE_CHUNK_Z_COL = "chunk_z";
    private static final String LEAVES_TABLE_X_COL = "x";
    private static final String LEAVES_TABLE_Y_COL = "y";
    private static final String LEAVES_TABLE_Z_COL = "z";
    private static final String LEAVES_TABLE_PERSISTENT_COL = "persistent";
    private static final String LEAVES_TABLE_DISTANCE_COL = "distance";
    private static final String LEAVES_TABLE_ACTUAL_PERSISTENCE_COL = "actual_persistence";
    private static final String LEAVES_TABLE_ACTUAL_DISTANCE_COL = "actual_distance";
    private static final String LEAVES_TABLE_MATERIAL_COL = "material";
    private static final String LEAVES_CREATE_TABLE_STATEMENT =
            "CREATE TABLE IF NOT EXISTS " + LEAVES_TABLE_NAME + "(" +
                    LEAVES_TABLE_WORLD_UUID_COL + " BINARY(16) NOT NULL, " +
                    LEAVES_TABLE_CHUNK_X_COL + " INTEGER NOT NULL, " +
                    LEAVES_TABLE_CHUNK_Z_COL + " INTEGER NOT NULL, " +
                    LEAVES_TABLE_X_COL + " INTEGER NOT NULL, " +
                    LEAVES_TABLE_Y_COL + " INTEGER NOT NULL, " +
                    LEAVES_TABLE_Z_COL + " INTEGER NOT NULL, " +
                    LEAVES_TABLE_PERSISTENT_COL + " BOOLEAN NOT NULL, " +
                    LEAVES_TABLE_DISTANCE_COL + " INTEGER NOT NULL, " +
                    LEAVES_TABLE_ACTUAL_PERSISTENCE_COL + " BOOLEAN NOT NULL, " +
                    LEAVES_TABLE_ACTUAL_DISTANCE_COL + " INTEGER NOT NULL, " +
                    LEAVES_TABLE_MATERIAL_COL + " TEXT NOT NULL, " +
                    "PRIMARY KEY (" + LEAVES_TABLE_X_COL + ", " + LEAVES_TABLE_Y_COL + ", " + LEAVES_TABLE_Z_COL + ")" +
                    ");";

    private static final String LOGS_TABLE_NAME = "logs";
    private static final String LOGS_TABLE_WORLD_UUID_COL = "world_uuid";
    private static final String LOGS_TABLE_CHUNK_X_COL = "chunk_x";
    private static final String LOGS_TABLE_CHUNK_Z_COL = "chunk_z";
    private static final String LOGS_TABLE_X_COL = "x";
    private static final String LOGS_TABLE_Y_COL = "y";
    private static final String LOGS_TABLE_Z_COL = "z";
    private static final String LOGS_CREATE_TABLE_STATEMENT =
            "CREATE TABLE IF NOT EXISTS " + LOGS_TABLE_NAME + "(" +
                    LOGS_TABLE_WORLD_UUID_COL + " BINARY(16) NOT NULL, " +
                    LOGS_TABLE_CHUNK_X_COL + " INTEGER NOT NULL, " +
                    LOGS_TABLE_CHUNK_Z_COL + " INTEGER NOT NULL, " +
                    LOGS_TABLE_X_COL + " INTEGER NOT NULL, " +
                    LOGS_TABLE_Y_COL + " INTEGER NOT NULL, " +
                    LOGS_TABLE_Z_COL + " INTEGER NOT NULL, " +
                    "PRIMARY KEY (" + LOGS_TABLE_X_COL + ", " + LOGS_TABLE_Y_COL + ", " + LOGS_TABLE_Z_COL + ")" +
                    ");";

    private static final String SET_LEAF_STATEMENT =
            "INSERT OR REPLACE INTO " + LEAVES_TABLE_NAME + " (" +
                    LEAVES_TABLE_WORLD_UUID_COL + ", " +
                    LEAVES_TABLE_CHUNK_X_COL + ", " +
                    LEAVES_TABLE_CHUNK_Z_COL + ", " +
                    LEAVES_TABLE_X_COL + ", " +
                    LEAVES_TABLE_Y_COL + ", " +
                    LEAVES_TABLE_Z_COL + ", " +
                    LEAVES_TABLE_PERSISTENT_COL + ", " +
                    LEAVES_TABLE_DISTANCE_COL + ", " +
                    LEAVES_TABLE_ACTUAL_PERSISTENCE_COL + ", " +
                    LEAVES_TABLE_ACTUAL_DISTANCE_COL +
                    LEAVES_TABLE_MATERIAL_COL +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

    private static final String GET_LEAF_STATEMENT =
            "SELECT " +
                    LEAVES_TABLE_PERSISTENT_COL + ", " +
                    LEAVES_TABLE_DISTANCE_COL + ", " +
                    LEAVES_TABLE_ACTUAL_PERSISTENCE_COL + ", " +
                    LEAVES_TABLE_ACTUAL_DISTANCE_COL + ", " +
                    LEAVES_TABLE_MATERIAL_COL +
                    " FROM " + LEAVES_TABLE_NAME + " WHERE " +
                    LEAVES_TABLE_WORLD_UUID_COL + " = ? AND " +
                    LEAVES_TABLE_X_COL + " = ? AND " +
                    LEAVES_TABLE_Y_COL + " = ? AND " +
                    LEAVES_TABLE_Z_COL + " = ?;";

    private static final String GET_LEAVES_IN_CHUNK_STATEMENT =
            "SELECT " +
                    LEAVES_TABLE_X_COL + ", " +
                    LEAVES_TABLE_Y_COL + ", " +
                    LEAVES_TABLE_Z_COL + ", " +
                    LEAVES_TABLE_PERSISTENT_COL + ", " +
                    LEAVES_TABLE_DISTANCE_COL + ", " +
                    LEAVES_TABLE_ACTUAL_PERSISTENCE_COL + ", " +
                    LEAVES_TABLE_ACTUAL_DISTANCE_COL + ", " +
                    LEAVES_TABLE_MATERIAL_COL +
                    " FROM " + LEAVES_TABLE_NAME + " WHERE " +
                    LEAVES_TABLE_WORLD_UUID_COL + " = ? AND " +
                    LEAVES_TABLE_CHUNK_X_COL + " = ? AND " +
                    LEAVES_TABLE_CHUNK_Z_COL + " = ?;";

    private static final String DELETE_LEAVES_IN_CHUNK_STATEMENT =
            "DELETE FROM " + LEAVES_TABLE_NAME + " WHERE " +
                    LEAVES_TABLE_WORLD_UUID_COL + " = ? AND " +
                    LEAVES_TABLE_CHUNK_X_COL + " = ? AND " +
                    LEAVES_TABLE_CHUNK_Z_COL + " = ?;";

    private static final String DELETE_LEAF_STATEMENT =
            "DELETE FROM " + LEAVES_TABLE_NAME + " WHERE " +
                    LEAVES_TABLE_WORLD_UUID_COL + " = ? AND " +
                    LEAVES_TABLE_X_COL + " = ? AND " +
                    LEAVES_TABLE_Y_COL + " = ? AND " +
                    LEAVES_TABLE_Z_COL + " = ?;";

    private static final String SET_LOG_STATEMENT =
            "INSERT OR REPLACE INTO " + LOGS_TABLE_NAME + " (" +
                    LOGS_TABLE_WORLD_UUID_COL + ", " +
                    LOGS_TABLE_CHUNK_X_COL + ", " +
                    LOGS_TABLE_CHUNK_Z_COL + ", " +
                    LOGS_TABLE_X_COL + ", " +
                    LOGS_TABLE_Y_COL + ", " +
                    LOGS_TABLE_Z_COL + ", " +
                    ") VALUES (?, ?, ?, ?, ?, ?);";

    private static final String GET_LOG_STATEMENT =
            "SELECT " +
                    " FROM " + LOGS_TABLE_NAME + " WHERE " +
                    LOGS_TABLE_WORLD_UUID_COL + " = ? AND " +
                    LOGS_TABLE_X_COL + " = ? AND " +
                    LOGS_TABLE_Y_COL + " = ? AND " +
                    LOGS_TABLE_Z_COL + " = ?;";

    private static final String GET_LOGS_IN_CHUNK_STATEMENT =
            "SELECT " +
                    LOGS_TABLE_X_COL + ", " +
                    LOGS_TABLE_Y_COL + ", " +
                    LOGS_TABLE_Z_COL + ", " +
                    " FROM " + LOGS_TABLE_NAME + " WHERE " +
                    LOGS_TABLE_WORLD_UUID_COL + " = ? AND " +
                    LOGS_TABLE_CHUNK_X_COL + " = ? AND " +
                    LOGS_TABLE_CHUNK_Z_COL + " = ?;";

    private static final String DELETE_LOGS_IN_CHUNK_STATEMENT =
            "DELETE FROM " + LOGS_TABLE_NAME + " WHERE " +
                    LOGS_TABLE_WORLD_UUID_COL + " = ? AND " +
                    LOGS_TABLE_CHUNK_X_COL + " = ? AND " +
                    LOGS_TABLE_CHUNK_Z_COL + " = ?;";

    private static final String DELETE_LOG_STATEMENT =
            "DELETE FROM " + LOGS_TABLE_NAME + " WHERE " +
                    LOGS_TABLE_WORLD_UUID_COL + " = ? AND " +
                    LOGS_TABLE_X_COL + " = ? AND " +
                    LOGS_TABLE_Y_COL + " = ? AND " +
                    LOGS_TABLE_Z_COL + " = ?;";

    public void createTables() {
        try (final Connection connection = this.getConnection()) {
            if (connection == null) throw new IllegalStateException("Connection is null! (createTables())");
            try (final PreparedStatement statement = connection.prepareStatement(LEAVES_CREATE_TABLE_STATEMENT)) {
                statement.execute();
            }
            try (final PreparedStatement statement = connection.prepareStatement(LOGS_CREATE_TABLE_STATEMENT)) {
                statement.execute();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveLeaf(Position2D chunkPos, Position position, FakeLeafState state) {
        try (final Connection connection = this.getConnection()) {
            if (connection == null) throw new IllegalStateException("Connection is null! (setLeaf())");
            try (final PreparedStatement statement = connection.prepareStatement(SET_LEAF_STATEMENT)) {
                statement.setBytes(1, uuidToBytes(chunkPos.world()));
                statement.setInt(2, chunkPos.x());
                statement.setInt(3, chunkPos.y());
                statement.setInt(4, position.x());
                statement.setInt(5, position.y());
                statement.setInt(6, position.z());
                statement.setBoolean(7, state.state().isPersistent());
                statement.setInt(8, state.state().getDistance());
                statement.setBoolean(9, state.actuallyPersistent());
                statement.setInt(10, state.actualDistance());
                statement.setString(11, state.material().toString());
                statement.execute();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveLeaves(Map<Position, FakeLeafState> leaves) {
        try (final Connection connection = this.getConnection()) {
            if (connection == null) throw new IllegalStateException("Connection is null! (setLeaves())");
            try (final PreparedStatement statement = connection.prepareStatement(SET_LEAF_STATEMENT)) {
                for (Map.Entry<Position, FakeLeafState> entry : leaves.entrySet()) {
                    final Position position = entry.getKey();
                    final int chunkX = position.x() >> 4;
                    final int chunkZ = position.z() >> 4;
                    statement.setBytes(1, uuidToBytes(position.world()));
                    statement.setInt(2, chunkX);
                    statement.setInt(3, chunkZ);
                    statement.setInt(4, entry.getKey().x());
                    statement.setInt(5, entry.getKey().y());
                    statement.setInt(6, entry.getKey().z());
                    statement.setBoolean(7, entry.getValue().state().isPersistent());
                    statement.setInt(8, entry.getValue().state().getDistance());
                    statement.setBoolean(9, entry.getValue().actuallyPersistent());
                    statement.setInt(10, entry.getValue().actualDistance());
                    statement.setString(11, entry.getValue().material().toString());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<Position, FakeLeafState> loadLeavesInChunk(Position2D position2D) {
        final Map<Position, FakeLeafState> leaves = new HashMap<>();
        try (final Connection connection = this.getConnection()) {
            if (connection == null) throw new IllegalStateException("Connection is null! (loadLeaveInChunk())");
            try (final PreparedStatement statement = connection.prepareStatement(GET_LEAVES_IN_CHUNK_STATEMENT)) {
                statement.setBytes(1, uuidToBytes(position2D.world()));
                statement.setInt(2, position2D.x());
                statement.setInt(3, position2D.y());
                try (final ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        Position position = new Position(position2D.world(), resultSet.getInt(1), resultSet.getInt(2), resultSet.getInt(3));
                        final Material material = Material.valueOf(resultSet.getString(8));
                        final FakeLeafState state = this.plugin.config().getDefaultState(material);
                        state.state().setPersistent(resultSet.getBoolean(4));
                        state.state().setDistance(resultSet.getInt(5));
                        state.actuallyPersistent(resultSet.getBoolean(6));
                        state.actualDistance(resultSet.getInt(7));
                        leaves.put(position, state);
                    }
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        return leaves;
    }

    public void deleteLeaf(Position2D chunkPos, Position position) {
        try (final Connection connection = this.getConnection()) {
            if (connection == null) throw new IllegalStateException("Connection is null! (deleteLeaf())");
            try (final PreparedStatement statement = connection.prepareStatement(DELETE_LEAF_STATEMENT)) {
                statement.setBytes(1, uuidToBytes(chunkPos.world()));
                statement.setInt(2, chunkPos.x());
                statement.setInt(3, chunkPos.y());
                statement.setInt(4, position.x());
                statement.setInt(5, position.y());
                statement.setInt(6, position.z());
                statement.execute();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteLeaves(Collection<Position> leafPositions) {
        try (final Connection connection = this.getConnection()) {
            if (connection == null) throw new IllegalStateException("Connection is null! (deleteLeaves())");
            try (final PreparedStatement statement = connection.prepareStatement(DELETE_LEAF_STATEMENT)) {
                for (Position position : leafPositions) {
                    statement.setBytes(1, uuidToBytes(position.world()));
                    statement.setInt(2, position.x());
                    statement.setInt(3, position.y());
                    statement.setInt(4, position.z());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteLeavesInChunk(Position2D chunkPos) {
        try (final Connection connection = this.getConnection()) {
            if (connection == null) throw new IllegalStateException("Connection is null! (deleteLeaves())");
            try (final PreparedStatement statement = connection.prepareStatement(DELETE_LEAVES_IN_CHUNK_STATEMENT)) {
                statement.setBytes(1, uuidToBytes(chunkPos.world()));
                statement.setInt(2, chunkPos.x());
                statement.setInt(3, chunkPos.y());
                statement.execute();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveLog(Position2D chunkPos, Position position, Material material) {
        try (final Connection connection = this.getConnection()) {
            if (connection == null) throw new IllegalStateException("Connection is null! (setLog())");
            try (final PreparedStatement statement = connection.prepareStatement(SET_LOG_STATEMENT)) {
                statement.setBytes(1, uuidToBytes(chunkPos.world()));
                statement.setInt(2, chunkPos.x());
                statement.setInt(3, chunkPos.y());
                statement.setInt(4, position.x());
                statement.setInt(5, position.y());
                statement.setInt(6, position.z());
                statement.execute();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveLogs(Collection<Position> logs) {
        try (final Connection connection = this.getConnection()) {
            if (connection == null) throw new IllegalStateException("Connection is null! (setLogs())");
            try (final PreparedStatement statement = connection.prepareStatement(SET_LOG_STATEMENT)) {
                for (Position position : logs) {
                    final int chunkX = position.x() >> 4;
                    final int chunkZ = position.z() >> 4;
                    statement.setBytes(1, uuidToBytes(position.world()));
                    statement.setInt(2, chunkX);
                    statement.setInt(3, chunkZ);
                    statement.setInt(4, position.x());
                    statement.setInt(5, position.y());
                    statement.setInt(6, position.z());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public Collection<Position> loadLogsInChunk(Position2D position2D) {
        final Collection<Position> logs = new HashSet<>();
        try (final Connection connection = this.getConnection()) {
            if (connection == null) throw new IllegalStateException("Connection is null! (loadLogsInChunk())");
            try (final PreparedStatement statement = connection.prepareStatement(GET_LOGS_IN_CHUNK_STATEMENT)) {
                statement.setBytes(1, uuidToBytes(position2D.world()));
                statement.setInt(2, position2D.x());
                statement.setInt(3, position2D.y());
                try (final ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        final Position position = new Position(position2D.world(), resultSet.getInt(1), resultSet.getInt(2), resultSet.getInt(3));
                        logs.add(position);
                    }
                }
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public void deleteLog(Position2D chunkPos, Position position) {
        try (final Connection connection = this.getConnection()) {
            if (connection == null) throw new IllegalStateException("Connection is null! (deleteLog())");
            try (final PreparedStatement statement = connection.prepareStatement(DELETE_LOG_STATEMENT)) {
                statement.setBytes(1, uuidToBytes(chunkPos.world()));
                statement.setInt(2, chunkPos.x());
                statement.setInt(3, chunkPos.y());
                statement.setInt(4, position.x());
                statement.setInt(5, position.y());
                statement.setInt(6, position.z());
                statement.execute();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteLogs(Collection<Position> logPositions) {
        try (final Connection connection = this.getConnection()) {
            if (connection == null) throw new IllegalStateException("Connection is null! (deleteLogs())");
            try (final PreparedStatement statement = connection.prepareStatement(DELETE_LOG_STATEMENT)) {
                for (Position position : logPositions) {
                    statement.setBytes(1, uuidToBytes(position.world()));
                    statement.setInt(2, position.x());
                    statement.setInt(3, position.y());
                    statement.setInt(4, position.z());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteLogsInChunk(Position2D chunkPos) {
        try (final Connection connection = this.getConnection()) {
            if (connection == null) throw new IllegalStateException("Connection is null! (deleteLogs())");
            try (final PreparedStatement statement = connection.prepareStatement(DELETE_LOGS_IN_CHUNK_STATEMENT)) {
                statement.setBytes(1, uuidToBytes(chunkPos.world()));
                statement.setInt(2, chunkPos.x());
                statement.setInt(3, chunkPos.y());
                statement.execute();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
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
