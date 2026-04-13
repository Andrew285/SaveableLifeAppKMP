package org.simpleapps.saveablekmp.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbDir = File(System.getProperty("user.home"), ".saveable")
        dbDir.mkdirs()
        val dbFile = File(dbDir, "saveable.db")
        val isNew = !dbFile.exists() || dbFile.length() == 0L
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        if (isNew) {
            SaveableDatabase.Schema.create(driver)
        }
        return driver
    }
}