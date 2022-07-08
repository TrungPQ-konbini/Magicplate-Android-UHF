package com.konbini.magicplateuhf.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `plateModels` ADD COLUMN `last_plate_serial` TEXT");
        }
    }
    val MIGRATION_1_3 = object : Migration(1, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("DROP TABLE IF EXISTS `users`")
            database.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `displayName` TEXT NOT NULL, `roles` TEXT NOT NULL, `ccwId1` TEXT NOT NULL, `ccwId2` TEXT NOT NULL, `ccwId3` TEXT NOT NULL)")
        }
    }
    val MIGRATION_1_4 = object : Migration(1, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `products` ADD COLUMN `color` TEXT")
        }
    }
}