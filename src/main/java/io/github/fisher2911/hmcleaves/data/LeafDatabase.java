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

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.cache.ChunkBlockCache;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.util.ChunkUtil;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LeafDatabase {

    private final HMCLeaves plugin;
    private final LeavesConfig config;
    private final Path databaseFilePath;
    private final ExecutorService writeExecutor;
    private final ExecutorService readExecutor;
    private Connection connection;
    // 528*528 chunks, not 16x16 chunks
    private final Map<UUID, Multimap<ChunkPosition, Integer>> possibleWorldDefaultLayers;
    private final Set<ChunkPosition> currentlyLoadingChunks;

    public LeafDatabase(HMCLeaves plugin) {
        this.plugin = plugin;
        this.config = plugin.getLeavesConfig();
        this.databaseFilePath = this.plugin.getDataFolder().toPath().resolve("database").resolve("leaves.db");
        this.writeExecutor = Executors.newSingleThreadExecutor();
        this.readExecutor = Executors.newFixedThreadPool(5);
        this.possibleWorldDefaultLayers = new HashMap<>();
        this.currentlyLoadingChunks = ConcurrentHashMap.newKeySet();
    }

    public boolean isLayerLoaded(ChunkPosition smallChunk) {
        return !this.getPossibleWorldDefaultLayers(smallChunk).isEmpty() &&
                !this.currentlyLoadingChunks.contains(smallChunk.toLargeChunk());
    }

    public Collection<Integer> getPossibleWorldDefaultLayers(ChunkPosition smallChunk) {
        final int largeChunkX = ChunkUtil.getLargeChunkCoordFromChunkCoord(smallChunk.x());
        final int largeChunkZ = ChunkUtil.getLargeChunkCoordFromChunkCoord(smallChunk.z());
        final UUID worldUUID = smallChunk.world();
        final ChunkPosition largeChunk = new ChunkPosition(worldUUID, largeChunkX, largeChunkZ);
        final Multimap<ChunkPosition, Integer> layers = possibleWorldDefaultLayers.get(largeChunk.world());
        if (layers == null) return Collections.emptyList();
        return layers.get(largeChunk);
    }

    @Nullable
    private Connection getConnection() {
        if (this.connection != null) return this.connection;
        final File folder = this.databaseFilePath.getParent().toFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.databaseFilePath);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not connect to database!", e);
        }
        return this.connection;
    }

    public void load() {
        this.createTables();
    }

    public void doDatabaseWriteAsync(Runnable runnable) {
        if (this.writeExecutor.isShutdown() || this.writeExecutor.isTerminated()) {
            runnable.run();
            return;
        }
        this.writeExecutor.execute(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void doDatabaseReadAsync(Runnable runnable) {
        this.readExecutor.execute(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
//        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, runnable);
    }

    private static final String LOADED_CHUNKS_TABLE_NAME = "loaded_chunks";
    private static final String LOADED_CHUNKS_TABLE_WORLD_UUID_COLUMN = "world_uuid";
    private static final String LOADED_CHUNKS_TABLE_CHUNK_X_COLUMN = "chunk_x";
    private static final String LOADED_CHUNKS_TABLE_CHUNK_Z_COLUMN = "chunk_z";
    private static final String LOADED_CHUNKS_TABLE_CHUNK_VERSION_COLUMN = "chunk_version";
    private static final String CREATE_LOADED_CHUNKS_TABLE_STATEMENT =
            "CREATE TABLE IF NOT EXISTS " + LOADED_CHUNKS_TABLE_NAME + " (" +
                    LOADED_CHUNKS_TABLE_WORLD_UUID_COLUMN + " BINARY(16) NOT NULL, " +
                    LOADED_CHUNKS_TABLE_CHUNK_X_COLUMN + " INTEGER NOT NULL, " +
                    LOADED_CHUNKS_TABLE_CHUNK_Z_COLUMN + " INTEGER NOT NULL, " +
                    LOADED_CHUNKS_TABLE_CHUNK_VERSION_COLUMN + " INTEGER NOT NULL, " +
                    "PRIMARY KEY (" + LOADED_CHUNKS_TABLE_WORLD_UUID_COLUMN + ", " + LOADED_CHUNKS_TABLE_CHUNK_X_COLUMN + ", " + LOADED_CHUNKS_TABLE_CHUNK_Z_COLUMN + ")" +
                    ");";

    private static final String GET_CHUNK_VERSION_STATEMENT =
            "SELECT " + LOADED_CHUNKS_TABLE_CHUNK_VERSION_COLUMN + " FROM " + LOADED_CHUNKS_TABLE_NAME + " WHERE " +
                    LOADED_CHUNKS_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    LOADED_CHUNKS_TABLE_CHUNK_X_COLUMN + " = ? AND " +
                    LOADED_CHUNKS_TABLE_CHUNK_Z_COLUMN + " = ?;";

    private static final String INSERT_CHUNK_VERSION_STATEMENT =
            "INSERT OR REPLACE INTO " + LOADED_CHUNKS_TABLE_NAME + " (" +
                    LOADED_CHUNKS_TABLE_WORLD_UUID_COLUMN + ", " +
                    LOADED_CHUNKS_TABLE_CHUNK_X_COLUMN + ", " +
                    LOADED_CHUNKS_TABLE_CHUNK_Z_COLUMN + ", " +
                    LOADED_CHUNKS_TABLE_CHUNK_VERSION_COLUMN + ") VALUES (?, ?, ?, ?);";

    // stores all layers that have default leaf data
    private static final String DEFAULT_CHUNK_DATA_LAYERS_TABLE_NAME = "default_chunk_data_y_layers";
    private static final String DEFAULT_CHUNK_DATA_LAYERS_WORLD_UUID_COLUMN = "world_uuid";
    // instead of 16x16 chunks, use 528*528 chunks
    private static final String DEFAULT_CHUNK_DATA_LAYERS_LARGE_CHUNK_X_COLUMN = "chunk_x";
    private static final String DEFAULT_CHUNK_DATA_LAYERS_LARGE_CHUNK_Z_COLUMN = "chunk_z";
    private static final String DEFAULT_CHUNK_DATA_LAYERS_Y_COLUMN = "y";
    private static final String CREATE_DEFAULT_CHUNK_DATA_LAYERS_TABLE_STATEMENT =
            "CREATE TABLE IF NOT EXISTS " + DEFAULT_CHUNK_DATA_LAYERS_TABLE_NAME + " (" +
                    DEFAULT_CHUNK_DATA_LAYERS_WORLD_UUID_COLUMN + " BINARY(16) NOT NULL, " +
                    DEFAULT_CHUNK_DATA_LAYERS_LARGE_CHUNK_X_COLUMN + " INTEGER NOT NULL, " +
                    DEFAULT_CHUNK_DATA_LAYERS_LARGE_CHUNK_Z_COLUMN + " INTEGER NOT NULL, " +
                    DEFAULT_CHUNK_DATA_LAYERS_Y_COLUMN + " INTEGER NOT NULL, " +
                    "UNIQUE(" + DEFAULT_CHUNK_DATA_LAYERS_WORLD_UUID_COLUMN + ", " +
                    DEFAULT_CHUNK_DATA_LAYERS_LARGE_CHUNK_X_COLUMN + ", " +
                    DEFAULT_CHUNK_DATA_LAYERS_LARGE_CHUNK_Z_COLUMN + ", "
                    + DEFAULT_CHUNK_DATA_LAYERS_Y_COLUMN + ")" +
                    ");";
    //    private static final String CREATE_DEFAULT_CHUNK_DATA_LAYERS_INDEX_STATEMENT =
//            "CREATE INDEX IF NOT EXISTS " + DEFAULT_CHUNK_DATA_LAYERS_TABLE_NAME + "_index ON " + DEFAULT_CHUNK_DATA_LAYERS_TABLE_NAME + " (" +
//                    DEFAULT_CHUNK_DATA_LAYERS_WORLD_UUID_COLUMN + ", " +
//                    DEFAULT_CHUNK_DATA_LAYERS_CHUNK_X_COLUMN + ", " +
//                    DEFAULT_CHUNK_DATA_LAYERS_CHUNK_Z_COLUMN + ");";
    private static final String GET_DEFAULT_CHUNK_DATA_LAYERS_STATEMENT =
            "SELECT " + DEFAULT_CHUNK_DATA_LAYERS_Y_COLUMN + " FROM " + DEFAULT_CHUNK_DATA_LAYERS_TABLE_NAME + " WHERE " +
                    DEFAULT_CHUNK_DATA_LAYERS_WORLD_UUID_COLUMN + " = ? AND " +
                    DEFAULT_CHUNK_DATA_LAYERS_LARGE_CHUNK_X_COLUMN + " = ? AND " +
                    DEFAULT_CHUNK_DATA_LAYERS_LARGE_CHUNK_Z_COLUMN + " = ?;";
    //                    DEFAULT_CHUNK_DATA_LAYERS_CHUNK_X_COLUMN + " = ? AND " +
//                    DEFAULT_CHUNK_DATA_LAYERS_CHUNK_Z_COLUMN + " = ?;";
    private static final String INSERT_DEFAULT_CHUNK_DATA_LAYERS_STATEMENT =
            "INSERT OR REPLACE INTO " + DEFAULT_CHUNK_DATA_LAYERS_TABLE_NAME + " (" +
                    DEFAULT_CHUNK_DATA_LAYERS_WORLD_UUID_COLUMN + ", " +
                    DEFAULT_CHUNK_DATA_LAYERS_LARGE_CHUNK_X_COLUMN + ", " +
                    DEFAULT_CHUNK_DATA_LAYERS_LARGE_CHUNK_Z_COLUMN + ", " +
                    DEFAULT_CHUNK_DATA_LAYERS_Y_COLUMN + ") VALUES (?, ?, ?, ?);";

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

    private static final String DELETE_LEAF_CHUNK_STATEMENT =
            "DELETE FROM " + LEAVES_TABLE_NAME + " WHERE " +
                    LEAVES_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    LEAVES_TABLE_CHUNK_X_COLUMN + " = ? AND " +
                    LEAVES_TABLE_CHUNK_Z_COLUMN + " = ?;";


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

    private static final String DELETE_LOG_CHUNK_STATEMENT =
            "DELETE FROM " + LOGS_TABLE_NAME + " WHERE " +
                    LOGS_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    LOGS_TABLE_CHUNK_X_COLUMN + " = ? AND " +
                    LOGS_TABLE_CHUNK_Z_COLUMN + " = ?;";

    private static final String SAPLINGS_TABLE_NAME = "saplings";
    private static final String SAPLINGS_TABLE_WORLD_UUID_COLUMN = "world_uuid";
    private static final String SAPLINGS_TABLE_CHUNK_X_COLUMN = "chunk_x";
    private static final String SAPLINGS_TABLE_CHUNK_Z_COLUMN = "chunk_z";
    private static final String SAPLINGS_TABLE_BLOCK_X_COLUMN = "block_x";
    private static final String SAPLINGS_TABLE_BLOCK_Y_COLUMN = "block_y";
    private static final String SAPLINGS_TABLE_BLOCK_Z_COLUMN = "block_z";
    private static final String SAPLINGS_TABLE_BLOCK_ID_COLUMN = "block_id";
    private static final String CREATE_SAPLINGS_TABLE_STATEMENT =
            "CREATE TABLE IF NOT EXISTS " + SAPLINGS_TABLE_NAME + " (" +
                    SAPLINGS_TABLE_WORLD_UUID_COLUMN + " BINARY(16) NOT NULL, " +
                    SAPLINGS_TABLE_CHUNK_X_COLUMN + " INTEGER NOT NULL, " +
                    SAPLINGS_TABLE_CHUNK_Z_COLUMN + " INTEGER NOT NULL, " +
                    SAPLINGS_TABLE_BLOCK_X_COLUMN + " INTEGER NOT NULL, " +
                    SAPLINGS_TABLE_BLOCK_Y_COLUMN + " INTEGER NOT NULL, " +
                    SAPLINGS_TABLE_BLOCK_Z_COLUMN + " INTEGER NOT NULL, " +
                    SAPLINGS_TABLE_BLOCK_ID_COLUMN + " TEXT NOT NULL, " +
                    "PRIMARY KEY (" + SAPLINGS_TABLE_WORLD_UUID_COLUMN + ", " + SAPLINGS_TABLE_BLOCK_X_COLUMN + ", " + SAPLINGS_TABLE_BLOCK_Y_COLUMN + ", " + SAPLINGS_TABLE_BLOCK_Z_COLUMN + ")" +
                    ");";

    public static final String SET_SAPLING_BLOCK_STATEMENT =
            "INSERT OR REPLACE INTO " + SAPLINGS_TABLE_NAME + " (" +
                    SAPLINGS_TABLE_WORLD_UUID_COLUMN + ", " +
                    SAPLINGS_TABLE_CHUNK_X_COLUMN + ", " +
                    SAPLINGS_TABLE_CHUNK_Z_COLUMN + ", " +
                    SAPLINGS_TABLE_BLOCK_X_COLUMN + ", " +
                    SAPLINGS_TABLE_BLOCK_Y_COLUMN + ", " +
                    SAPLINGS_TABLE_BLOCK_Z_COLUMN + ", " +
                    SAPLINGS_TABLE_BLOCK_ID_COLUMN +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?);";

    public static final String GET_SAPLING_BLOCKS_IN_CHUNK_STATEMENT =
            "SELECT " + SAPLINGS_TABLE_BLOCK_X_COLUMN + ", " + SAPLINGS_TABLE_BLOCK_Y_COLUMN + ", " + SAPLINGS_TABLE_BLOCK_Z_COLUMN + ", " + SAPLINGS_TABLE_BLOCK_ID_COLUMN + " FROM " + SAPLINGS_TABLE_NAME + " WHERE " +
                    SAPLINGS_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    SAPLINGS_TABLE_CHUNK_X_COLUMN + " = ? AND " +
                    SAPLINGS_TABLE_CHUNK_Z_COLUMN + " = ?;";

    public static final String DELETE_SAPLING_BLOCK_STATEMENT =
            "DELETE FROM " + SAPLINGS_TABLE_NAME + " WHERE " +
                    SAPLINGS_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    SAPLINGS_TABLE_BLOCK_X_COLUMN + " = ? AND " +
                    SAPLINGS_TABLE_BLOCK_Y_COLUMN + " = ? AND " +
                    SAPLINGS_TABLE_BLOCK_Z_COLUMN + " = ?;";

    public static final String DELETE_SAPLING_CHUNK_STATEMENT =
            "DELETE FROM " + SAPLINGS_TABLE_NAME + " WHERE " +
                    SAPLINGS_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    SAPLINGS_TABLE_CHUNK_X_COLUMN + " = ? AND " +
                    SAPLINGS_TABLE_CHUNK_Z_COLUMN + " = ?;";

    private static final String CAVE_VINES_TABLE_NAME = "cave_vines";
    private static final String CAVE_VINES_TABLE_WORLD_UUID_COLUMN = "world_uuid";
    private static final String CAVE_VINES_TABLE_CHUNK_X_COLUMN = "chunk_x";
    private static final String CAVE_VINES_TABLE_CHUNK_Z_COLUMN = "chunk_z";
    private static final String CAVE_VINES_TABLE_BLOCK_X_COLUMN = "block_x";
    private static final String CAVE_VINES_TABLE_BLOCK_Y_COLUMN = "block_y";
    private static final String CAVE_VINES_TABLE_BLOCK_Z_COLUMN = "block_z";
    private static final String CAVE_VINES_TABLE_BLOCK_ID_COLUMN = "block_id";
    private static final String CAVE_VINES_TABLE_HAS_BERRIES_COLUMN = "has_berries";
    private static final String CREATE_CAVE_VINES_TABLE_STATEMENT =
            "CREATE TABLE IF NOT EXISTS " + CAVE_VINES_TABLE_NAME + " (" +
                    CAVE_VINES_TABLE_WORLD_UUID_COLUMN + " BINARY(16) NOT NULL, " +
                    CAVE_VINES_TABLE_CHUNK_X_COLUMN + " INTEGER NOT NULL, " +
                    CAVE_VINES_TABLE_CHUNK_Z_COLUMN + " INTEGER NOT NULL, " +
                    CAVE_VINES_TABLE_BLOCK_X_COLUMN + " INTEGER NOT NULL, " +
                    CAVE_VINES_TABLE_BLOCK_Y_COLUMN + " INTEGER NOT NULL, " +
                    CAVE_VINES_TABLE_BLOCK_Z_COLUMN + " INTEGER NOT NULL, " +
                    CAVE_VINES_TABLE_BLOCK_ID_COLUMN + " TEXT NOT NULL, " +
                    CAVE_VINES_TABLE_HAS_BERRIES_COLUMN + " BOOLEAN NOT NULL, " +
                    "PRIMARY KEY (" + CAVE_VINES_TABLE_WORLD_UUID_COLUMN + ", " + CAVE_VINES_TABLE_BLOCK_X_COLUMN + ", " + CAVE_VINES_TABLE_BLOCK_Y_COLUMN + ", " + CAVE_VINES_TABLE_BLOCK_Z_COLUMN + ")" +
                    ");";

    public static final String SET_CAVE_VINES_BLOCK_STATEMENT =
            "INSERT OR REPLACE INTO " + CAVE_VINES_TABLE_NAME + " (" +
                    CAVE_VINES_TABLE_WORLD_UUID_COLUMN + ", " +
                    CAVE_VINES_TABLE_CHUNK_X_COLUMN + ", " +
                    CAVE_VINES_TABLE_CHUNK_Z_COLUMN + ", " +
                    CAVE_VINES_TABLE_BLOCK_X_COLUMN + ", " +
                    CAVE_VINES_TABLE_BLOCK_Y_COLUMN + ", " +
                    CAVE_VINES_TABLE_BLOCK_Z_COLUMN + ", " +
                    CAVE_VINES_TABLE_BLOCK_ID_COLUMN + ", " +
                    CAVE_VINES_TABLE_HAS_BERRIES_COLUMN +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?);";

    public static final String GET_CAVE_VINES_BLOCKS_IN_CHUNK_STATEMENT =
            "SELECT " + CAVE_VINES_TABLE_BLOCK_X_COLUMN + ", " + CAVE_VINES_TABLE_BLOCK_Y_COLUMN + ", " + CAVE_VINES_TABLE_BLOCK_Z_COLUMN + ", " + CAVE_VINES_TABLE_BLOCK_ID_COLUMN + ", " + CAVE_VINES_TABLE_HAS_BERRIES_COLUMN + " FROM " + CAVE_VINES_TABLE_NAME + " WHERE " +
                    CAVE_VINES_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    CAVE_VINES_TABLE_CHUNK_X_COLUMN + " = ? AND " +
                    CAVE_VINES_TABLE_CHUNK_Z_COLUMN + " = ?;";

    public static final String DELETE_CAVE_VINES_BLOCK_STATEMENT =
            "DELETE FROM " + CAVE_VINES_TABLE_NAME + " WHERE " +
                    CAVE_VINES_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    CAVE_VINES_TABLE_BLOCK_X_COLUMN + " = ? AND " +
                    CAVE_VINES_TABLE_BLOCK_Y_COLUMN + " = ? AND " +
                    CAVE_VINES_TABLE_BLOCK_Z_COLUMN + " = ?;";

    public static final String DELETE_CAVE_VINES_CHUNK_STATEMENT =
            "DELETE FROM " + CAVE_VINES_TABLE_NAME + " WHERE " +
                    CAVE_VINES_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    CAVE_VINES_TABLE_CHUNK_X_COLUMN + " = ? AND " +
                    CAVE_VINES_TABLE_CHUNK_Z_COLUMN + " = ?;";

    private static final String AGEABLE_TABLE_NAME = "ageable";
    private static final String AGEABLE_TABLE_WORLD_UUID_COLUMN = "world_uuid";
    private static final String AGEABLE_TABLE_CHUNK_X_COLUMN = "chunk_x";
    private static final String AGEABLE_TABLE_CHUNK_Z_COLUMN = "chunk_z";
    private static final String AGEABLE_TABLE_BLOCK_X_COLUMN = "block_x";
    private static final String AGEABLE_TABLE_BLOCK_Y_COLUMN = "block_y";
    private static final String AGEABLE_TABLE_BLOCK_Z_COLUMN = "block_z";
    private static final String AGEABLE_TABLE_BLOCK_ID_COLUMN = "block_id";
    private static final String CREATE_AGEABLE_TABLE_STATEMENT =
            "CREATE TABLE IF NOT EXISTS " + AGEABLE_TABLE_NAME + " (" +
                    AGEABLE_TABLE_WORLD_UUID_COLUMN + " BINARY(16) NOT NULL, " +
                    AGEABLE_TABLE_CHUNK_X_COLUMN + " INTEGER NOT NULL, " +
                    AGEABLE_TABLE_CHUNK_Z_COLUMN + " INTEGER NOT NULL, " +
                    AGEABLE_TABLE_BLOCK_X_COLUMN + " INTEGER NOT NULL, " +
                    AGEABLE_TABLE_BLOCK_Y_COLUMN + " INTEGER NOT NULL, " +
                    AGEABLE_TABLE_BLOCK_Z_COLUMN + " INTEGER NOT NULL, " +
                    AGEABLE_TABLE_BLOCK_ID_COLUMN + " TEXT NOT NULL, " +
                    "PRIMARY KEY (" + AGEABLE_TABLE_WORLD_UUID_COLUMN + ", " + AGEABLE_TABLE_BLOCK_X_COLUMN + ", " + AGEABLE_TABLE_BLOCK_Y_COLUMN + ", " + AGEABLE_TABLE_BLOCK_Z_COLUMN + ")" +
                    ");";

    private static final String SET_AGEABLE_BLOCK_STATEMENT =
            "INSERT OR REPLACE INTO " + AGEABLE_TABLE_NAME + " (" +
                    AGEABLE_TABLE_WORLD_UUID_COLUMN + ", " +
                    AGEABLE_TABLE_CHUNK_X_COLUMN + ", " +
                    AGEABLE_TABLE_CHUNK_Z_COLUMN + ", " +
                    AGEABLE_TABLE_BLOCK_X_COLUMN + ", " +
                    AGEABLE_TABLE_BLOCK_Y_COLUMN + ", " +
                    AGEABLE_TABLE_BLOCK_Z_COLUMN + ", " +
                    AGEABLE_TABLE_BLOCK_ID_COLUMN +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?);";

    private static final String GET_AGEABLE_BLOCKS_IN_CHUNK_STATEMENT =
            "SELECT " + AGEABLE_TABLE_BLOCK_X_COLUMN + ", " + AGEABLE_TABLE_BLOCK_Y_COLUMN + ", " + AGEABLE_TABLE_BLOCK_Z_COLUMN + ", " + AGEABLE_TABLE_BLOCK_ID_COLUMN + " FROM " + AGEABLE_TABLE_NAME + " WHERE " +
                    AGEABLE_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    AGEABLE_TABLE_CHUNK_X_COLUMN + " = ? AND " +
                    AGEABLE_TABLE_CHUNK_Z_COLUMN + " = ?;";

    private static final String DELETE_AGEABLE_BLOCK_STATEMENT =
            "DELETE FROM " + AGEABLE_TABLE_NAME + " WHERE " +
                    AGEABLE_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    AGEABLE_TABLE_BLOCK_X_COLUMN + " = ? AND " +
                    AGEABLE_TABLE_BLOCK_Y_COLUMN + " = ? AND " +
                    AGEABLE_TABLE_BLOCK_Z_COLUMN + " = ?;";

    private static final String DELETE_AGEABLE_CHUNK_STATEMENT =
            "DELETE FROM " + AGEABLE_TABLE_NAME + " WHERE " +
                    AGEABLE_TABLE_WORLD_UUID_COLUMN + " = ? AND " +
                    AGEABLE_TABLE_CHUNK_X_COLUMN + " = ? AND " +
                    AGEABLE_TABLE_CHUNK_Z_COLUMN + " = ?;";


    private void createTables() {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        try (final PreparedStatement statement = connection.prepareStatement(CREATE_DEFAULT_CHUNK_DATA_LAYERS_TABLE_STATEMENT)) {
            statement.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create tables!", e);
        }
//        try (final PreparedStatement statement = connection.prepareStatement(CREATE_DEFAULT_CHUNK_DATA_LAYERS_INDEX_STATEMENT)) {
//            statement.execute();
//        } catch (SQLException e) {
//            throw new IllegalStateException("Could not create tables!", e);
//        }
        try (final PreparedStatement statement = connection.prepareStatement(CREATE_LOADED_CHUNKS_TABLE_STATEMENT)) {
            statement.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create tables!", e);
        }
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
        try (final PreparedStatement statement = connection.prepareStatement(CREATE_SAPLINGS_TABLE_STATEMENT)) {
            statement.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create tables!", e);
        }
        try (final PreparedStatement statement = connection.prepareStatement(CREATE_CAVE_VINES_TABLE_STATEMENT)) {
            statement.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create tables!", e);
        }
        try (final PreparedStatement statement = connection.prepareStatement(CREATE_AGEABLE_TABLE_STATEMENT)) {
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

    public boolean isChunkLoaded(ChunkPosition chunkPosition) {
        try (final PreparedStatement statement = this.connection.prepareStatement(GET_CHUNK_VERSION_STATEMENT)) {
            statement.setBytes(1, this.uuidToBytes(chunkPosition.world()));
            statement.setInt(2, chunkPosition.x());
            statement.setInt(3, chunkPosition.z());
            try (final ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(LOADED_CHUNKS_TABLE_CHUNK_VERSION_COLUMN) == this.config.getChunkVersion();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setChunkLoaded(ChunkPosition chunkPosition) {
        try {
            final Connection connection = this.getConnection();
            if (connection == null) throw new IllegalStateException("Could not connect to database!");
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(INSERT_CHUNK_VERSION_STATEMENT)) {
                statement.setBytes(1, this.uuidToBytes(chunkPosition.world()));
                statement.setInt(2, chunkPosition.x());
                statement.setInt(3, chunkPosition.z());
                statement.setInt(4, this.config.getChunkVersion());
                statement.execute();
            }
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Runnable> shutdownNow() {
        this.readExecutor.shutdown();
        return this.writeExecutor.shutdownNow();
    }

    public void saveBlocksInChunk(ChunkBlockCache chunk) {
        try {
            chunk.setSaving(true);
            this.saveLeafBlocksInChunk(chunk);
            this.saveLogBlocksInChunk(chunk);
            this.saveSaplingBlocksInChunk(chunk);
            this.saveCaveVineBlocksInChunk(chunk);
            this.saveAgeableBlocksInChunk(chunk);
            chunk.setSaving(false);
            chunk.markClean();
            chunk.setSafeToMarkClean(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteChunk(ChunkPosition chunkPosition) {
        try {
            this.deleteLeafChunk(chunkPosition);
            this.deleteLogChunk(chunkPosition);
            this.deleteSaplingChunk(chunkPosition);
            this.deleteCaveVinesChunk(chunkPosition);
            this.deleteAgeableChunk(chunkPosition);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<Position, BlockData> getBlocksInChunk(ChunkPosition chunkPosition, LeavesConfig config) {
        final Map<Position, BlockData> blocks = new HashMap<>();
//        Debugger.getInstance().logChunkLoadTaskStart(chunkPosition);
        blocks.putAll(this.getLeafBlocksInChunk(chunkPosition, config));
        blocks.putAll(this.getLogBlocksInChunk(chunkPosition, config));
        blocks.putAll(this.getSaplingBlocksInChunk(chunkPosition, config));
        blocks.putAll(this.getCaveVineBlocksInChunk(chunkPosition, config));
        blocks.putAll(this.getAgeableBlocksInChunk(chunkPosition, config));
//        Debugger.getInstance().logChunkLoadEnd(chunkPosition, blocks.size());
        return blocks;
    }

    public void saveDefaultDataLayers(UUID worldUUID, Collection<Integer> yLayers, ChunkPosition smallChunk) throws SQLException {
        if (yLayers.isEmpty()) return;
        final ChunkPosition largeChunk = smallChunk.toLargeChunk();
        final Multimap<ChunkPosition, Integer> multimap = this.possibleWorldDefaultLayers
        .computeIfAbsent(worldUUID, uuid -> Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet));
        multimap.putAll(largeChunk, yLayers);
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final byte[] worldUUIDBytes = this.uuidToBytes(worldUUID);
        this.connection.setAutoCommit(false);
        try (final PreparedStatement statement = connection.prepareStatement(INSERT_DEFAULT_CHUNK_DATA_LAYERS_STATEMENT)) {
            for (int y : yLayers) {
                statement.setBytes(1, worldUUIDBytes);
                statement.setInt(2, largeChunk.x());
                statement.setInt(3, largeChunk.z());
                statement.setInt(4, y);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save default data layers!", e);
        }
        connection.commit();
    }

    public void loadAllDefaultPossibleLayersInWorld(UUID worldUUID, ChunkPosition smallChunk) {
        final List<Integer> yLevels = new ArrayList<>();
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final byte[] worldUUIDBytes = this.uuidToBytes(worldUUID);
        final ChunkPosition largeChunk = smallChunk.toLargeChunk();
        this.currentlyLoadingChunks.add(largeChunk);
        try (final PreparedStatement statement = connection.prepareStatement(GET_DEFAULT_CHUNK_DATA_LAYERS_STATEMENT)) {
            statement.setBytes(1, worldUUIDBytes);
            statement.setInt(2, largeChunk.x());
            statement.setInt(3, largeChunk.z());
            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    yLevels.add(resultSet.getInt(DEFAULT_CHUNK_DATA_LAYERS_Y_COLUMN));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not get all possible layers in chunk!", e);
        }
        final Multimap<ChunkPosition, Integer> multimap = this.possibleWorldDefaultLayers
        .computeIfAbsent(worldUUID, uuid -> Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet));
        multimap.putAll(largeChunk, yLevels);
        this.currentlyLoadingChunks.remove(largeChunk);
    }

    private void saveLeafBlocksInChunk(ChunkBlockCache chunk) throws SQLException {
        this.deleteRemovedLeafBlocksInChunk(chunk);
        if (chunk.getBlockDataMap().isEmpty()) return;
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final ChunkPosition chunkPosition = chunk.getChunkPosition();
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        this.connection.setAutoCommit(false);
        try (final PreparedStatement statement = connection.prepareStatement(SET_LEAF_BLOCK_STATEMENT)) {
            for (var entry : chunk.getBlockDataMap().entrySet()) {
                final Position position = entry.getKey();
                final int blockX = position.x();
                final int blockY = position.y();
                final int blockZ = position.z();
                final BlockData blockData = entry.getValue();
                if (!blockData.shouldSave()) continue;
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
        this.connection.commit();
    }

    private void deleteRemovedLeafBlocksInChunk(ChunkBlockCache chunkBlockCache) {
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

    private void deleteLeafChunk(ChunkPosition chunkPosition) throws SQLException {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        connection.setAutoCommit(false);
        try (final PreparedStatement statement = connection.prepareStatement(DELETE_LEAF_CHUNK_STATEMENT)) {
            statement.setBytes(1, worldUUIDBytes);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            statement.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete leaf chunk " + chunkX + ", " + chunkZ + "!", e);
        }
        connection.commit();
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

    private void saveLogBlocksInChunk(ChunkBlockCache chunkBlockCache) throws SQLException {
        this.deleteRemovedLogBlocksInChunk(chunkBlockCache);
        if (chunkBlockCache.getBlockDataMap().isEmpty()) return;
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final ChunkPosition chunkPosition = chunkBlockCache.getChunkPosition();
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        this.connection.setAutoCommit(false);
        try (final PreparedStatement statement = connection.prepareStatement(SET_LOG_BLOCK_STATEMENT)) {
            for (var entry : chunkBlockCache.getBlockDataMap().entrySet()) {
                final Position position = entry.getKey();
                final BlockData blockData = entry.getValue();
                if (!blockData.shouldSave()) continue;
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
        this.connection.commit();
    }

    private void deleteRemovedLogBlocksInChunk(ChunkBlockCache chunkBlockCache) {
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

    private void deleteLogChunk(ChunkPosition chunkPosition) throws SQLException {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        connection.setAutoCommit(false);
        try (final PreparedStatement statement = connection.prepareStatement(DELETE_LOG_CHUNK_STATEMENT)) {
            statement.setBytes(1, worldUUIDBytes);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete log chunk " + chunkX + ", " + chunkZ + "!", e);
        }
        connection.commit();
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

    private void saveSaplingBlocksInChunk(ChunkBlockCache chunk) throws SQLException {
        this.deleteRemovedSaplingsInChunk(chunk);
        if (chunk.getBlockDataMap().isEmpty()) return;
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final ChunkPosition chunkPosition = chunk.getChunkPosition();
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        this.connection.setAutoCommit(false);
        try (final PreparedStatement statement = connection.prepareStatement(SET_SAPLING_BLOCK_STATEMENT)) {
            for (var entry : chunk.getBlockDataMap().entrySet()) {
                final Position position = entry.getKey();
                final int blockX = position.x();
                final int blockY = position.y();
                final int blockZ = position.z();
                final BlockData blockData = entry.getValue();
                if (!blockData.shouldSave()) continue;
                if (!(blockData instanceof SaplingData)) continue;
                final String blockID = blockData.id();
                statement.setBytes(1, worldUUIDBytes);
                statement.setInt(2, chunkX);
                statement.setInt(3, chunkZ);
                statement.setInt(4, blockX);
                statement.setInt(5, blockY);
                statement.setInt(6, blockZ);
                statement.setString(7, blockID);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save saplings in chunk " + chunkX + ", " + chunkZ + "!", e);
        }
        this.connection.commit();
    }

    private void deleteRemovedSaplingsInChunk(ChunkBlockCache chunkBlockCache) {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final ChunkPosition chunkPosition = chunkBlockCache.getChunkPosition();
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        try (final PreparedStatement statement = connection.prepareStatement(DELETE_SAPLING_BLOCK_STATEMENT)) {
            chunkBlockCache.clearRemovedPositions(entry -> {
                try {
                    final Position position = entry.getKey();
                    final BlockData blockData = entry.getValue();
                    if (!(blockData instanceof SaplingData)) return false;
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
                    throw new IllegalStateException("Could not delete removed saplings in chunk " + chunkX + ", " + chunkZ + "!", e);
                }
            });
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete removed saplings in chunk " + chunkX + ", " + chunkZ + "!", e);
        }
    }

    private void deleteSaplingChunk(ChunkPosition chunkPosition) throws SQLException {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        connection.setAutoCommit(false);
        try (final PreparedStatement statement = connection.prepareStatement(DELETE_SAPLING_CHUNK_STATEMENT)) {
            statement.setBytes(1, worldUUIDBytes);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete sapling chunk " + chunkX + ", " + chunkZ + "!", e);
        }
        connection.commit();
    }

    private Map<Position, BlockData> getSaplingBlocksInChunk(ChunkPosition chunkPosition, LeavesConfig config) {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        final Map<Position, BlockData> saplingBlocks = new HashMap<>();
        try (final PreparedStatement statement = connection.prepareStatement(GET_SAPLING_BLOCKS_IN_CHUNK_STATEMENT)) {
            statement.setBytes(1, worldUUIDBytes);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    final int blockX = resultSet.getInt(SAPLINGS_TABLE_BLOCK_X_COLUMN);
                    final int blockY = resultSet.getInt(SAPLINGS_TABLE_BLOCK_Y_COLUMN);
                    final int blockZ = resultSet.getInt(SAPLINGS_TABLE_BLOCK_Z_COLUMN);
                    final String blockID = resultSet.getString(SAPLINGS_TABLE_BLOCK_ID_COLUMN);
                    final Position position = new Position(chunkPosition.world(), blockX, blockY, blockZ);
                    final BlockData blockData = config.getBlockData(blockID);
                    if (!(blockData instanceof SaplingData saplingData)) {
                        this.plugin.getLogger().warning("Could not find block data for block ID " + blockID + " at position " +
                                blockX + ", " + blockY + ", " + blockZ + "!");
                        continue;
                    }
                    saplingBlocks.put(position, saplingData);
                }
            }
            return saplingBlocks;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not get sapling blocks in chunk " + chunkX + ", " + chunkZ + "!", e);
        }
    }

    private void saveCaveVineBlocksInChunk(ChunkBlockCache chunk) throws SQLException {
        this.deleteRemovedCaveVineBlocksInChunk(chunk);
        if (chunk.getBlockDataMap().isEmpty()) return;
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final ChunkPosition chunkPosition = chunk.getChunkPosition();
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        this.connection.setAutoCommit(false);
        try (final PreparedStatement statement = connection.prepareStatement(SET_CAVE_VINES_BLOCK_STATEMENT)) {
            for (var entry : chunk.getBlockDataMap().entrySet()) {
                final Position position = entry.getKey();
                final int blockX = position.x();
                final int blockY = position.y();
                final int blockZ = position.z();
                final BlockData blockData = entry.getValue();
                if (!blockData.shouldSave()) continue;
                if (!(blockData instanceof final CaveVineData caveVineData)) continue;
                final String blockID = blockData.id();
                statement.setBytes(1, worldUUIDBytes);
                statement.setInt(2, chunkX);
                statement.setInt(3, chunkZ);
                statement.setInt(4, blockX);
                statement.setInt(5, blockY);
                statement.setInt(6, blockZ);
                statement.setString(7, blockID);
                statement.setBoolean(8, caveVineData.glowBerry());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save cave vine blocks in chunk " + chunkX + ", " + chunkZ + "!", e);
        }
        this.connection.commit();
    }

    private void deleteRemovedCaveVineBlocksInChunk(ChunkBlockCache chunkBlockCache) {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final ChunkPosition chunkPosition = chunkBlockCache.getChunkPosition();
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        try (final PreparedStatement statement = connection.prepareStatement(DELETE_CAVE_VINES_BLOCK_STATEMENT)) {
            chunkBlockCache.clearRemovedPositions(entry -> {
                try {
                    final Position position = entry.getKey();
                    final BlockData blockData = entry.getValue();
                    if (!(blockData instanceof CaveVineData)) return false;
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
                    throw new IllegalStateException("Could not delete removed cave vine blocks in chunk " + chunkX + ", " + chunkZ + "!", e);
                }
            });
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete removed cave vine blocks in chunk " + chunkX + ", " + chunkZ + "!", e);
        }
    }

    private void deleteCaveVinesChunk(ChunkPosition chunkPosition) throws SQLException {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        connection.setAutoCommit(false);
        try (final PreparedStatement statement = connection.prepareStatement(DELETE_CAVE_VINES_CHUNK_STATEMENT)) {
            statement.setBytes(1, worldUUIDBytes);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete cave vines chunk " + chunkX + ", " + chunkZ + "!", e);
        }
        connection.commit();
    }

    private Map<Position, BlockData> getCaveVineBlocksInChunk(ChunkPosition chunkPosition, LeavesConfig config) {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        final Map<Position, BlockData> caveVineBlocks = new HashMap<>();
        try (final PreparedStatement statement = connection.prepareStatement(GET_CAVE_VINES_BLOCKS_IN_CHUNK_STATEMENT)) {
            statement.setBytes(1, worldUUIDBytes);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    final int blockX = resultSet.getInt(CAVE_VINES_TABLE_BLOCK_X_COLUMN);
                    final int blockY = resultSet.getInt(CAVE_VINES_TABLE_BLOCK_Y_COLUMN);
                    final int blockZ = resultSet.getInt(CAVE_VINES_TABLE_BLOCK_Z_COLUMN);
                    final String blockID = resultSet.getString(CAVE_VINES_TABLE_BLOCK_ID_COLUMN);
                    final boolean glowBerry = resultSet.getBoolean(CAVE_VINES_TABLE_HAS_BERRIES_COLUMN);
                    final Position position = new Position(chunkPosition.world(), blockX, blockY, blockZ);
                    final BlockData blockData = config.getBlockData(blockID);
                    if (!(blockData instanceof CaveVineData caveVineData)) {
                        this.plugin.getLogger().warning("Could not find block data for block ID " + blockID + " at position " +
                                blockX + ", " + blockY + ", " + blockZ + "!");
                        continue;
                    }
                    caveVineBlocks.put(position, caveVineData.withGlowBerry(glowBerry));
                }
            }
            return caveVineBlocks;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not get cave vine blocks in chunk " + chunkX + ", " + chunkZ + "!", e);
        }
    }

    private void saveAgeableBlocksInChunk(ChunkBlockCache chunk) throws SQLException {
        this.deleteRemovedAgeableBlocksInChunk(chunk);
        if (chunk.getBlockDataMap().isEmpty()) return;
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final ChunkPosition chunkPosition = chunk.getChunkPosition();
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        this.connection.setAutoCommit(false);
        try (final PreparedStatement statement = connection.prepareStatement(SET_AGEABLE_BLOCK_STATEMENT)) {
            for (var entry : chunk.getBlockDataMap().entrySet()) {
                final Position position = entry.getKey();
                final int blockX = position.x();
                final int blockY = position.y();
                final int blockZ = position.z();
                final BlockData blockData = entry.getValue();
                if (!blockData.shouldSave()) continue;
                if (!(blockData instanceof AgeableData)) continue;
                final String blockID = blockData.id();
                statement.setBytes(1, worldUUIDBytes);
                statement.setInt(2, chunkX);
                statement.setInt(3, chunkZ);
                statement.setInt(4, blockX);
                statement.setInt(5, blockY);
                statement.setInt(6, blockZ);
                statement.setString(7, blockID);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save ageable blocks in chunk " + chunkX + ", " + chunkZ + "!", e);
        }
        this.connection.commit();
    }

    private void deleteRemovedAgeableBlocksInChunk(ChunkBlockCache chunkBlockCache) {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final ChunkPosition chunkPosition = chunkBlockCache.getChunkPosition();
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        try (final PreparedStatement statement = connection.prepareStatement(DELETE_AGEABLE_BLOCK_STATEMENT)) {
            chunkBlockCache.clearRemovedPositions(entry -> {
                try {
                    final Position position = entry.getKey();
                    final BlockData blockData = entry.getValue();
                    if (!(blockData instanceof AgeableData)) return false;
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
                    throw new IllegalStateException("Could not delete removed ageable blocks in chunk " + chunkX + ", " + chunkZ + "!", e);
                }
            });
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete removed ageable blocks in chunk " + chunkX + ", " + chunkZ + "!", e);
        }
    }

    private void deleteAgeableChunk(ChunkPosition chunkPosition) throws SQLException {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        connection.setAutoCommit(false);
        try (final PreparedStatement statement = connection.prepareStatement(DELETE_AGEABLE_CHUNK_STATEMENT)) {
            statement.setBytes(1, worldUUIDBytes);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            statement.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete ageable chunk " + chunkX + ", " + chunkZ + "!", e);
        }
        connection.commit();
    }

    private Map<Position, BlockData> getAgeableBlocksInChunk(ChunkPosition chunkPosition, LeavesConfig config) {
        final Connection connection = this.getConnection();
        if (connection == null) throw new IllegalStateException("Could not connect to database!");
        final byte[] worldUUIDBytes = this.uuidToBytes(chunkPosition.world());
        final int chunkX = chunkPosition.x();
        final int chunkZ = chunkPosition.z();
        final Map<Position, BlockData> ageableBlocks = new HashMap<>();
        try (final PreparedStatement statement = connection.prepareStatement(GET_AGEABLE_BLOCKS_IN_CHUNK_STATEMENT)) {
            statement.setBytes(1, worldUUIDBytes);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    final int blockX = resultSet.getInt(AGEABLE_TABLE_BLOCK_X_COLUMN);
                    final int blockY = resultSet.getInt(AGEABLE_TABLE_BLOCK_Y_COLUMN);
                    final int blockZ = resultSet.getInt(AGEABLE_TABLE_BLOCK_Z_COLUMN);
                    final String blockID = resultSet.getString(AGEABLE_TABLE_BLOCK_ID_COLUMN);
                    final Position position = new Position(chunkPosition.world(), blockX, blockY, blockZ);
                    final BlockData blockData = config.getBlockData(blockID);
                    if (!(blockData instanceof AgeableData ageableData)) {
                        this.plugin.getLogger().warning("Could not find block data for block ID " + blockID + " at position " +
                                blockX + ", " + blockY + ", " + blockZ + "!");
                        continue;
                    }
                    ageableBlocks.put(position, ageableData);
                }
            }
            return ageableBlocks;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not get ageable blocks in chunk " + chunkX + ", " + chunkZ + "!", e);
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
