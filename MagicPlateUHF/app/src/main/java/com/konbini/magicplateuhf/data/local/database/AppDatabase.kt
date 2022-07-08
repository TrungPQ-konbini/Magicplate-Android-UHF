package com.konbini.magicplateuhf.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.konbini.magicplateuhf.data.entities.*
import com.konbini.magicplateuhf.data.local.category.CategoryDao
import com.konbini.magicplateuhf.data.local.menu.MenuDao
import com.konbini.magicplateuhf.data.local.offlineData.OfflineDataDao
import com.konbini.magicplateuhf.data.local.plateModel.PlateModelDao
import com.konbini.magicplateuhf.data.local.product.ProductDao
import com.konbini.magicplateuhf.data.local.timeBlock.TimeBlockDao
import com.konbini.magicplateuhf.data.local.transaction.TransactionDao
import com.konbini.magicplateuhf.data.local.user.UserDao

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        PlateModelEntity::class,
        TimeBlockEntity::class,
        MenuEntity::class,
        OfflineDataEntity::class,
        TransactionEntity::class,
        UserEntity::class
    ], version = 4, exportSchema = false
)
abstract class AppDatabase: RoomDatabase() {

    // Add DAO
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun plateModelDao(): PlateModelDao
    abstract fun timeBlockDao(): TimeBlockDao
    abstract fun menuDao(): MenuDao
    abstract fun offlineDataDao(): OfflineDataDao
    abstract fun transactionDao(): TransactionDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        private fun buildDatabase(appContext: Context) =
            Room.databaseBuilder(appContext, AppDatabase::class.java, "MagicPlateUHF")
                .fallbackToDestructiveMigration()
                .addMigrations(Migration.MIGRATION_1_2)
                .addMigrations(Migration.MIGRATION_1_3)
                .addMigrations(Migration.MIGRATION_1_4)
                .build()
    }
}