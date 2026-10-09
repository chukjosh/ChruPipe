package com.chrupipe.database

import com.chrupipe.database.tables.*
import com.chrupipe.util.resolveDataDir
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {

    fun init() {
        // Store the database file in /app/data (Docker volume) or ./data (local)
        val dataDir = resolveDataDir()
        dataDir.mkdirs()

        val dbPath = "${dataDir.absolutePath}/newpipe.db"

        Database.connect(
            url = "jdbc:sqlite:$dbPath",
            driver = "org.sqlite.JDBC"
        )

        // Create all tables if they don't exist
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                HistoryTable,
                WatchlistTable,
                PlaylistsTable,
                PlaylistItemsTable,
                SubscriptionsTable,
                DownloadsTable
            )
        }
    }
}
