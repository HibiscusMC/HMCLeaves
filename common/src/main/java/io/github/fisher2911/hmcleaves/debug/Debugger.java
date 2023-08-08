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

package io.github.fisher2911.hmcleaves.debug;

import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.world.ChunkPosition;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Debugger {

    private static final Debugger INSTANCE = new Debugger(HMCLeaves.getPlugin(HMCLeaves.class));

    public static Debugger getInstance() {
        return INSTANCE;
    }

    private final HMCLeaves plugin;
    private final Path debugFolderPath;
    private final ExecutorService debugExecutor;

    private Debugger(HMCLeaves plugin) {
        this.plugin = plugin;
        this.debugFolderPath = this.plugin.getDataFolder().toPath().resolve("debug");
        this.debugExecutor = Executors.newSingleThreadExecutor();
    }

    public void logChunkLoadTaskStart(ChunkPosition chunkPos) {
        if (!this.plugin.debug()) return;
        this.debug("loadChunkStart: (" + chunkPos.x() + ", " + chunkPos.z() + ")");
    }

    public void logChunkBlockSendStart(ChunkPosition chunkPos) {
        if (!this.plugin.debug()) return;
        this.debug("sendChunkStart: (" + chunkPos.x() + ", " + chunkPos.z() + ")");
    }

    public void logChunkLayersLoad(ChunkPosition chunkPos, int layers) {
        if (!this.plugin.debug()) return;
        this.debug("layersLoad: (" + chunkPos.x() + ", " + chunkPos.z() + ") -> " + layers);
    }

    public void logChunkLoadEnd(ChunkPosition chunkPos, int totalBlocks) {
        if (!this.plugin.debug()) return;
        this.debug("loadChunkEnd: (" + chunkPos.x() + ", " + chunkPos.z() + ") -> " + totalBlocks);
    }

    public void logChunkBlockSendEnd(ChunkPosition chunkPos) {
        if (!this.plugin.debug()) return;
        this.debug("sendChunkEnd: (" + chunkPos.x() + ", " + chunkPos.z() + ")");
    }

    public void logTimeBetweenChunkLoadAndSend(ChunkPosition chunkPosition, LocalDateTime start, LocalDateTime end) {
        if (!this.plugin.debug()) return;
        this.debug("timeBetweenChunkLoadAndSend: (" + chunkPosition.x() + ", " + chunkPosition.z() + ") -> " + start.until(end, ChronoUnit.MILLIS) + "ms");
    }

    public void logMultiBlockSend(ChunkPosition chunkPosition, int playerCount, int blockCount, LocalDateTime time) {
        if (!this.plugin.debug()) return;
        this.debug("multiBlockSend: (" + chunkPosition.x() + ", " + chunkPosition.z() + ") -> " + playerCount + " -> " + blockCount + " -> " + TIME_FORMATTER.format(time) + "ms");
    }

    public void logChunkSend(ChunkPosition chunkPosition, LocalDateTime time) {
        if (!this.plugin.debug()) return;
        this.debug("chunkSend: (" + chunkPosition.x() + ", " + chunkPosition.z() + ") -> " + TIME_FORMATTER.format(time) + "ms");
    }


    private static final DateTimeFormatter DATE_TIME_FORMATTER_FILE_NAME = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss-SSS");

    private void debug(String message) {
        if (this.debugExecutor.isShutdown()) {
            this.doDebug(message);
            return;
        }
        this.debugExecutor.execute(() -> this.doDebug(message));
    }

    private void doDebug(String message) {
        final File debugFile = this.debugFolderPath.resolve(DATE_TIME_FORMATTER_FILE_NAME.format(LocalDateTime.now())).toFile();
        if (!debugFile.exists()) {
            try {
                Files.createDirectories(this.debugFolderPath);
                Files.createFile(debugFile.toPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            Files.writeString(
                    debugFile.toPath(),
                    "[" + TIME_FORMATTER.format(LocalTime.now()) + "]: " + message + "\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        this.debugExecutor.shutdownNow().forEach(Runnable::run);
    }

}
