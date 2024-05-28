package com.hibiscusmc.hmcleaves.debug

import com.hibiscusmc.hmcleaves.HMCLeaves
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.logging.Level

interface LeavesLogger {

    fun init()

    fun sendSynced(sendSynced: Boolean = true)

    fun info(message: String)

    fun warn(message: String)

    fun severe(message: String)

}

data object DisabledLeavesLogger : LeavesLogger {

    override fun init() {}

    override fun sendSynced(sendSynced: Boolean) {}

    override fun info(message: String) {}

    override fun warn(message: String) {}

    override fun severe(message: String) {}

}

class ActiveLeavesLogger(private val plugin: HMCLeaves) : LeavesLogger {

    private val executor = Executors.newFixedThreadPool(1)

    private lateinit var filePath: Path;

    private var sendSynced = false

    override fun sendSynced(sendSynced: Boolean) {
        this.sendSynced = sendSynced
    }

    override fun init() {
        val folderPath = plugin.dataFolder.toPath().resolve("logs")
        val dateTime = LocalDateTime.now()
        val formattedDay = "${dateTime.year}-${dateTime.monthValue}-${dateTime.dayOfMonth}"
        val filePath: Path = folderPath.resolve("${formattedDay}.txt")
        if (!filePath.toFile().exists()) {
            val file = filePath.toFile()
            file.parentFile.mkdirs()
            file.createNewFile()
            this.filePath = filePath
            return
        }
        var fileNumber = 0
        while (folderPath.resolve("${formattedDay}-${fileNumber}.txt").toFile().exists()) {
            fileNumber++
        }
        val file = folderPath.resolve("${formattedDay}-${fileNumber}.txt").toFile()
        file.parentFile.mkdirs()
        file.createNewFile()
    }

    override fun info(message: String) {
        this.writeMessage(Level.INFO, message)
    }

    override fun warn(message: String) {
        this.writeMessage(Level.WARNING, message)
    }

    override fun severe(message: String) {
        this.writeMessage(Level.SEVERE, message)
    }

    private fun writeMessage(logLevel: Level, message: String) {
        val time = LocalDateTime.now()
        val formattedTime = "${time.hour}:${time.minute}:${time.second}"
        val runnable = Runnable {
            Files.writeString(
                this.filePath,
                "[${formattedTime}] [${logLevel.name}]: ${message}\n",
                StandardOpenOption.APPEND
            )
        }
        if (this.sendSynced) {
            runnable.run()
            return
        }
        this.executor.execute(runnable)

    }

}