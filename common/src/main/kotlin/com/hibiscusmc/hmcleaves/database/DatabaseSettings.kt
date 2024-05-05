package com.hibiscusmc.hmcleaves.database

import java.nio.file.Path

enum class DatabaseType() {

    SQLITE,
    MYSQL,
    MONGODB

}

data class DatabaseSettings(
    val path: Path,
    val type: DatabaseType,
    val port: String,
    val user: String,
    val password: String
) {



}