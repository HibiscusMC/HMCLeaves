package com.hibiscusmc.hmcleaves.database

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.BlockGroup
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.util.toBytes
import com.hibiscusmc.hmcleaves.world.ChunkPosition
import com.hibiscusmc.hmcleaves.world.LeavesChunk
import com.hibiscusmc.hmcleaves.world.PositionInChunk
import com.hibiscusmc.hmcleaves.world.WorldManager
import org.bukkit.Bukkit
import org.bukkit.ChunkSnapshot
import org.bukkit.World
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


private const val LOADED_CHUNK_TABLE_NAME = "loaded_chunks"
private const val DEFAULT_BLOCK_GROUPS_TABLE_NAME = "default_block_groups"
private const val BLOCK_DATA_TABLE_NAME = "block_data"

private const val CHUNK_VERSION_COL = "chunk_version"

private const val WORLD_UUID_COL = "world_uuid"
private const val CHUNK_X_COL = "chunk_x"
private const val CHUNK_Z_COL = "chunk_z"
private const val BLOCK_X_COL = "block_x"
private const val BLOCK_Y_COL = "block_y"
private const val BLOCK_Z_COL = "block_z"
private const val BLOCK_ID_COL = "block_id"

private const val BLOCK_MIN_X_COL = "block_min_x"
private const val BLOCK_MIN_Y_COL = "block_min_y"
private const val BLOCK_MIN_Z_COL = "block_min_z"
private const val BLOCK_MAX_X_COL = "block_max_x"
private const val BLOCK_MAX_Y_COL = "block_max_y"
private const val BLOCK_MAX_Z_COL = "block_max_z"

private const val CREATE_LOADED_CHUNKS_TABLE_STATEMENT =
    "CREATE TABLE IF NOT EXISTS $LOADED_CHUNK_TABLE_NAME (" +
            "$WORLD_UUID_COL BINARY(16) NOT NULL, " +
            "$CHUNK_X_COL INTEGER NOT NULL, " +
            "$CHUNK_Z_COL INTEGER NOT NULL, " +
            "$CHUNK_VERSION_COL INTEGER NOT NULL, " +
            "PRIMARY KEY($WORLD_UUID_COL, $CHUNK_X_COL, $CHUNK_Z_COL)" +
            ");"
private const val INSERT_LOADED_CHUNK_STATEMENT = "INSERT OR REPLACE INTO $LOADED_CHUNK_TABLE_NAME (" +
        "$WORLD_UUID_COL, " +
        "$CHUNK_X_COL, " +
        "$CHUNK_Z_COL," +
        "$CHUNK_VERSION_COL) " +
        "VALUES (?,?,?,?)"
private const val SELECT_LOADED_CHUNK_STATEMENT = " SELECT $CHUNK_VERSION_COL " +
        "from $LOADED_CHUNK_TABLE_NAME " +
        "WHERE " +
        "$WORLD_UUID_COL=? AND " +
        "$CHUNK_X_COL=? AND " +
        "$CHUNK_Z_COL=?"

private const val CREATE_DEFAULT_BLOCK_GROUPS_TABLE_NAME =
    "CREATE TABLE IF NOT EXISTS $DEFAULT_BLOCK_GROUPS_TABLE_NAME (" +
            "$WORLD_UUID_COL BINARY(16) NOT NULL, " +
            "$CHUNK_X_COL INTEGER NOT NULL, " +
            "$CHUNK_Z_COL INTEGER NOT NULL, " +
            "$BLOCK_MIN_X_COL INTEGER NOT NULL, " +
            "$BLOCK_MIN_Y_COL INTEGER NOT NULL, " +
            "$BLOCK_MIN_Z_COL INTEGER NOT NULL, " +
            "$BLOCK_MAX_X_COL INTEGER NOT NULL, " +
            "$BLOCK_MAX_Y_COL INTEGER NOT NULL, " +
            "$BLOCK_MAX_Z_COL INTEGER NOT NULL);"

private const val INSERT_DEFAULT_GROUP_STATEMENT = "INSERT OR REPLACE INTO " +
        "$DEFAULT_BLOCK_GROUPS_TABLE_NAME (" +
        "$WORLD_UUID_COL, " +
        "$CHUNK_X_COL, " +
        "$CHUNK_Z_COL, " +
        "$BLOCK_MIN_X_COL, " +
        "$BLOCK_MIN_Y_COL, " +
        "$BLOCK_MIN_Z_COL, " +
        "$BLOCK_MAX_X_COL, " +
        "$BLOCK_MAX_Y_COL, " +
        "$BLOCK_MAX_Z_COL) " +
        "VALUES(?,?,?,?,?,?,?,?,?)"

private const val SELECT_DEFAULT_GROUP_STATEMENT = "SELECT " +
        "$BLOCK_MIN_X_COL, " +
        "$BLOCK_MIN_Y_COL, " +
        "$BLOCK_MIN_Z_COL, " +
        "$BLOCK_MAX_X_COL, " +
        "$BLOCK_MAX_Y_COL, " +
        "$BLOCK_MAX_Z_COL FROM " +
        "$DEFAULT_BLOCK_GROUPS_TABLE_NAME " +
        "WHERE " +
        "$WORLD_UUID_COL=? AND " +
        "$CHUNK_X_COL = ? AND " +
        "$CHUNK_Z_COL = ?;"

private const val CREATE_BLOCK_DATA_TABLE_STATEMENT = "CREATE TABLE IF NOT EXISTS $BLOCK_DATA_TABLE_NAME (" +
        "$WORLD_UUID_COL BINARY(16) NOT NULL, " +
        "$CHUNK_X_COL INTEGER NOT NULL, " +
        "$CHUNK_Z_COL INTEGER NOT NULL, " +
        "$BLOCK_X_COL INTEGER NOT NULL, " +
        "$BLOCK_Y_COL INTEGER NOT NULL, " +
        "$BLOCK_Z_COL INTEGER NOT NULL, " +
        "$BLOCK_ID_COL VARCHAR(255) NOT NULL, " +
        "PRIMARY KEY ($WORLD_UUID_COL, $CHUNK_X_COL, $CHUNK_Z_COL, $BLOCK_X_COL, $BLOCK_Y_COL, $BLOCK_Z_COL)" +
        ");"

private const val INSERT_BLOCK_DATA_STATEMENT = "INSERT OR REPLACE INTO " +
        "$BLOCK_DATA_TABLE_NAME (" +
        "$WORLD_UUID_COL, " +
        "$CHUNK_X_COL, " +
        "$CHUNK_Z_COL, " +
        "$BLOCK_X_COL, " +
        "$BLOCK_Y_COL, " +
        "$BLOCK_Z_COL, " +
        "$BLOCK_ID_COL) " +
        "VALUES(?,?,?,?,?,?,?)"

private const val SELECT_BLOCK_DATA_STATEMENT = "SELECT " +
        "$BLOCK_X_COL, " +
        "$BLOCK_Y_COL, " +
        "$BLOCK_Z_COL, " +
        "$BLOCK_ID_COL " +
        "FROM " +
        "$BLOCK_DATA_TABLE_NAME " +
        "WHERE " +
        "$WORLD_UUID_COL=? AND " +
        "$CHUNK_X_COL=? AND " +
        "$CHUNK_Z_COL=?;"


class SQLiteDatabase(
    private val plugin: HMCLeaves,
    private val worldManager: WorldManager = plugin.worldManager,
    private val config: LeavesConfig = plugin.leavesConfig
) : LeavesDatabase {

    private val chunkFirstLoadHandler = ChunkFirstLoadHandler(this.plugin)

    private val writeExecutor = Executors.newSingleThreadExecutor()
    private val readExecutor = Executors.newFixedThreadPool(5)
    private var connection: Connection? = null

    override val databaseExecutor = object : DatabaseExecutor {
        override fun executeRead(runnable: Runnable) {
            readExecutor.execute(runnable)
        }

        override fun executeWrite(runnable: Runnable) {
            writeExecutor.execute(runnable)
        }

    }

    private val chunksToRemoveCache: Cache<ChunkPosition, Boolean> by lazy {
        Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .removalListener { position: ChunkPosition?, value: Boolean?, cause: RemovalCause ->
                if (cause == RemovalCause.EXPLICIT) {
                    this.chunksToRemoveCache.put(position, value)
                    return@removalListener
                } // don't remove chunk if
                // it was removed from the cache in handleChunkLoad()
                if (position == null) return@removalListener
                worldManager[position.world]?.remove(position)
            }
            .build()
    }

    private fun createTable(connection: Connection, statement: String) {
        connection.prepareStatement(statement).use { preparedStatement ->
            preparedStatement.execute()
        }
    }

    private fun createTables(connection: Connection) {
        connection.autoCommit = false
        createTable(connection, CREATE_LOADED_CHUNKS_TABLE_STATEMENT)
        createTable(connection, CREATE_DEFAULT_BLOCK_GROUPS_TABLE_NAME)
        createTable(connection, CREATE_BLOCK_DATA_TABLE_STATEMENT)
        connection.commit()
    }

    override fun init() {
        val connection = getConnection() ?: throw IllegalStateException("Invalid connection")
        createTables(connection)
    }

    override fun handleChunkLoad(
        world: World,
        chunk: ChunkSnapshot,
        chunkPosition: ChunkPosition,
        async: Boolean
    ) {
        if (worldManager[world.uid]?.get(chunkPosition) != null) return
        this.chunksToRemoveCache.invalidate(chunkPosition)
        val connection = this.getConnection() ?: throw IllegalStateException("Invalid connection")
        val worldUUID = world.uid
        val worldUUIDBytes = worldUUID.toBytes()
        val chunkX = chunk.x
        val chunkZ = chunk.z
        val chunkVersion = this.config.getChunkVersion()
        connection.prepareStatement(SELECT_LOADED_CHUNK_STATEMENT).use { preparedStatement ->
            preparedStatement.setBytes(1, worldUUIDBytes)
            preparedStatement.setInt(2, chunkX)
            preparedStatement.setInt(3, chunkZ)
            val results = preparedStatement.executeQuery()
            if (!results.next() || results.getInt(CHUNK_VERSION_COL) != chunkVersion) {
                val leavesChunk = this.worldManager.getOrAdd(worldUUID).getOrAdd(chunkPosition)
                val groups = this.chunkFirstLoadHandler.load(world, chunk, leavesChunk)
                leavesChunk.setLoaded(true)
                if (async) {
                    this.writeExecutor.execute {
                        connection.autoCommit = false
                        this.saveBlockGroups(connection, chunkPosition, groups)
                        this.setChunkVersion(connection, worldUUIDBytes, chunkX, chunkZ)
                        connection.commit()
                        this.plugin.getLeavesLogger().info("Saved chunk on first load: (world=${world}, chunkX=${chunkX}, chunkZ=${chunkZ})")
                    }
                }
                return@use
            }
            this.loadChunk(connection, world, chunk, chunkPosition)
            return@use
        }
    }

    private fun saveBlockGroups(connection: Connection, chunkPosition: ChunkPosition, groups: Collection<BlockGroup>) {
        val worldUUID = chunkPosition.world
        val worldUUIDBytes = worldUUID.toBytes()
        val chunkX = chunkPosition.x
        val chunkZ = chunkPosition.z
        connection.prepareStatement(INSERT_DEFAULT_GROUP_STATEMENT).use { preparedStatement ->
            for (group in groups) {
                preparedStatement.setBytes(1, worldUUIDBytes)
                preparedStatement.setInt(2, chunkX)
                preparedStatement.setInt(3, chunkZ)
                preparedStatement.setInt(4, group.minX)
                preparedStatement.setInt(5, group.minY)
                preparedStatement.setInt(6, group.minZ)
                preparedStatement.setInt(7, group.maxX)
                preparedStatement.setInt(8, group.maxY)
                preparedStatement.setInt(9, group.maxZ)
                preparedStatement.addBatch()
            }
            preparedStatement.executeBatch()
        }
    }

    private fun setChunkVersion(connection: Connection, worldUUID: ByteArray, chunkX: Int, chunkZ: Int) {
        connection.prepareStatement(INSERT_LOADED_CHUNK_STATEMENT).use { preparedStatement ->
            preparedStatement.setBytes(1, worldUUID)
            preparedStatement.setInt(2, chunkX)
            preparedStatement.setInt(3, chunkZ)
            preparedStatement.setInt(4, this.config.getChunkVersion())
            preparedStatement.execute()
        }
    }

    private fun loadChunk(connection: Connection, world: World, chunk: ChunkSnapshot, chunkPosition: ChunkPosition) {
        val worldUUID = world.uid
        val worldUUIDBytes = worldUUID.toBytes()
        val chunkX = chunk.x
        val chunkZ = chunk.z

        val leavesChunk = worldManager.getOrAdd(worldUUID).getOrAdd(chunkPosition)

        connection.prepareStatement(SELECT_DEFAULT_GROUP_STATEMENT).use { preparedStatement ->
            preparedStatement.setBytes(1, worldUUIDBytes)
            preparedStatement.setInt(2, chunkX)
            preparedStatement.setInt(3, chunkZ)
            val results = preparedStatement.executeQuery()

            while (results.next()) {
                val minX = results.getInt(BLOCK_MIN_X_COL)
                val minY = results.getInt(BLOCK_MIN_Y_COL)
                val minZ = results.getInt(BLOCK_MIN_Z_COL)
                val maxX = results.getInt(BLOCK_MAX_X_COL)
                val maxY = results.getInt(BLOCK_MAX_Y_COL)
                val maxZ = results.getInt(BLOCK_MAX_Z_COL)
                loadBlockGroup(
                    leavesChunk,
                    chunk,
                    BlockGroup(
                        worldUUID,
                        minX,
                        minY,
                        minZ,
                        maxX,
                        maxY,
                        maxZ
                    )
                )
                plugin.getLeavesLogger().info("Loaded block group: (world=${worldUUID}, minX=${minX}, minY=${minY}, minZ=${minZ}, maxX=${maxX}, maxY=${maxY}, maxZ=${maxZ})")
            }
        }

        connection.prepareStatement(SELECT_BLOCK_DATA_STATEMENT).use { preparedStatement ->
            preparedStatement.setBytes(1, worldUUIDBytes)
            preparedStatement.setInt(2, chunkX)
            preparedStatement.setInt(3, chunkZ)
            val results = preparedStatement.executeQuery()
            while (results.next()) {
                val x = results.getInt(BLOCK_X_COL)
                val y = results.getInt(BLOCK_Y_COL)
                val z = results.getInt(BLOCK_Z_COL)
                val id = results.getString(BLOCK_ID_COL)

                val blockData = this.config.getBlockData(id) ?: continue
                val positionInChunk = PositionInChunk(worldUUID, x, y, z)
                leavesChunk[positionInChunk] = blockData
                plugin.logger.info("Loaded block data: (world=${worldUUID}, x=${x}, y=${y},  z=${z}, id=${id})")
            }
            leavesChunk.setLoaded(true)
            Bukkit.getScheduler().runTask(this.plugin) { _ ->
                for (player in Bukkit.getOnlinePlayers()) {
                    this.plugin.userManager.sendChunks(player)
                }
            }
        }
    }

    private fun loadBlockGroup(leavesChunk: LeavesChunk, chunk: ChunkSnapshot, group: BlockGroup) {
        val worldUUID = group.world
        for (x in group.minX..group.maxX) {
            for (y in group.minY..group.maxY) {
                for (z in group.minZ..group.maxZ) {
                    val block = chunk.getBlockData(x, y, z)
                    val defaultData = this.config.getDefaultBlockData(block.material) ?: continue
                    val positionInChunk = PositionInChunk(worldUUID, x, y, z)
                    leavesChunk.setDefaultBlock(positionInChunk, defaultData)
                }
            }
        }
    }

    override fun saveChunk(chunk: ChunkSnapshot, world: World, removeChunk: Boolean) {
        val worldUUID = world.uid
        val worldUUIDBytes = worldUUID.toBytes()
        val chunkX = chunk.x
        val chunkZ = chunk.z
        val chunkPosition = ChunkPosition(worldUUID, chunkX, chunkZ)
        val leavesChunk = this.worldManager[chunkPosition.world]?.get(chunkPosition) ?: return
        val connection = this.getConnection()
            ?: throw IllegalStateException("Invalid connection")
        connection.autoCommit = false
        connection.prepareStatement(INSERT_BLOCK_DATA_STATEMENT).use { preparedStatement ->
            for (entry in leavesChunk.getBlocks()) {
                val position = entry.key
                val data = entry.value ?: continue
                preparedStatement.setBytes(1, worldUUIDBytes)
                preparedStatement.setInt(2, chunkX)
                preparedStatement.setInt(3, chunkZ)
                preparedStatement.setInt(4, position.x)
                preparedStatement.setInt(5, position.y)
                preparedStatement.setInt(6, position.z)
                preparedStatement.setString(7, data.id)
                plugin.getLeavesLogger().info("Saving block data: (world=${worldUUID}, chunkX=${chunkX}, chunkZ=${chunkZ}, x=${position.x}, y=${position.y}, z=${position.z}, id=${data.id})")
                preparedStatement.addBatch()
            }
            preparedStatement.executeBatch()
        }
        if (removeChunk) {
            this.chunksToRemoveCache.put(chunkPosition, true)
        }
        connection.commit()
    }

    override fun saveWorld(world: World, removeChunks: Boolean) {
        for (chunk in world.loadedChunks) {
            this.saveChunk(chunk.chunkSnapshot, world, removeChunks)
        }
    }

    override fun loadWorld(world: World, async: Boolean) {
        for (chunk in world.loadedChunks) {
            val chunkPosition = ChunkPosition(world.uid, chunk.x, chunk.z)
            if (worldManager[world.uid]?.get(chunkPosition) != null) continue
            this.handleChunkLoad(world, chunk.chunkSnapshot, chunkPosition, async)
        }
    }

    private fun getConnection(): Connection? {
        if (this.connection != null) return this.connection
        val settings = this.config.getDatabaseSettings()
        val file: File = settings.path.toFile()
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + settings.path)
        } catch (e: SQLException) {
            throw IllegalStateException("Could not connect to database!", e)
        }
        return this.connection
    }

}