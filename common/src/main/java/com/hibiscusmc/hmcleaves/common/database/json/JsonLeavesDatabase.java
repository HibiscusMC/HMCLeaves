package com.hibiscusmc.hmcleaves.common.database.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.hibiscusmc.hmcleaves.common.HMCLeaves;
import com.hibiscusmc.hmcleaves.common.block.LeavesBlock;
import com.hibiscusmc.hmcleaves.common.config.LeavesConfig;
import com.hibiscusmc.hmcleaves.common.database.LeavesDatabase;
import com.hibiscusmc.hmcleaves.common.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.common.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.common.world.LeavesChunkSection;
import com.hibiscusmc.hmcleaves.common.world.Position;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class JsonLeavesDatabase implements LeavesDatabase {

    private final HMCLeaves hmcLeaves;
    private final LeavesConfig<?> config;
    private final Path folder;
    private final ExecutorService executorService;

    public JsonLeavesDatabase(HMCLeaves hmcLeaves, Path folder) {
        this.hmcLeaves = hmcLeaves;
        this.config = hmcLeaves.leavesConfig();
        this.folder = folder;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public @Nullable LeavesChunk loadChunk(UUID worldId, ChunkPosition chunkPosition) throws SQLException {
        final Path path = this.getChunkPath(worldId, chunkPosition);
        final File file = path.toFile();
        if (!file.exists()) {
            return null;
        }
        final Gson gson = new GsonBuilder()
                .create();
        final LeavesChunk chunk = new LeavesChunk(chunkPosition, new HashMap<>(), position -> new LeavesChunkSection(position, new ConcurrentHashMap<>()));
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(path))) {
            final LeavesJsonObject[] objects = gson.fromJson(reader, LeavesJsonObject[].class);
            if (objects == null) {
                return null;
            }
            for (LeavesJsonObject object : objects) {
                final Position position = new Position(worldId, object.x(), object.y(), object.z());
                final LeavesBlock block = this.config.getBlock(object.id());
                if (block == null) {
                    this.hmcLeaves.log(Level.WARNING, "Failed to load block with id " + object.id() + " at " + position + " from file.");
                    continue;
                }
                chunk.setBlock(position, block);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        chunk.setDirty(false);
        return chunk;
    }

    @Override
    public void saveChunk(LeavesChunk chunk) throws SQLException {
        final ChunkPosition chunkPosition = chunk.chunkPosition();
        final Path path = this.getChunkPath(chunkPosition.worldId(), chunkPosition);
        final File file = path.toFile();
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create chunk file", e);
            }
        }
        final Gson gson = new GsonBuilder()
                .create();
        final List<LeavesJsonObject> objects = new java.util.ArrayList<>();
        try (var writer = Files.newBufferedWriter(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (var section : chunk.getSections().values()) {
                for (var entry : section.getBlocks().entrySet()) {
                    final Position position = entry.getKey();
                    final LeavesBlock block = entry.getValue();
                    final LeavesJsonObject object = new LeavesJsonObject(block.id(), position);
                    objects.add(object);
                }
            }

            gson.toJson(objects, List.class, writer);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasChunkBeenScanned(UUID worldId, ChunkPosition position) throws SQLException {
        return this.getChunkPath(worldId, position).toFile().exists();
    }

    @Override
    public void setChunkScanned(UUID worldId, ChunkPosition position) throws SQLException {
        this.createChunkFile(worldId, position);
    }

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public void executeRead(Runnable runnable) {
        this.executorService.execute(runnable);
    }

    @Override
    public void executeWrite(Runnable runnable) {
        this.executorService.execute(runnable);
    }

    private Path getChunkPath(UUID worldId, ChunkPosition position) {
        return this.folder.resolve(worldId.toString()).resolve(position.x() + "," + position.z() + ".json");
    }

    private File createChunkFile(UUID worldId, ChunkPosition position) {
        final File file = this.getChunkPath(worldId, position).toFile();
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create chunk file", e);
            }
        }
        return file;
    }
}
