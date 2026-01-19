// Copyright 2024 anyone-Hub
@file:Suppress("RemoveRedundantQualifierName")

package com.anyonehub.barpos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MenuGroup::class,
        Category::class,
        MenuItem::class,
        ActiveTab::class,
        TabItem::class,
        AppSetting::class,
        User::class,
        TipLog::class,
        ZReportEntity::class,
        Sale::class,
        SaleItem::class,
        TimeClockEntry::class,
        Customer::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun posDao(): PosDao
    abstract fun reportDao(): ReportDao
    abstract fun menuDao(): MenuDao
    abstract fun salesDao(): SalesDao
    abstract fun timeClockDao(): TimeClockDao
    abstract fun customerDao(): CustomerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "midtown_pos_db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    seedDatabase(database.posDao())
                }
            }
        }

        private suspend fun seedDatabase(dao: PosDao) {
            // ADMIN SEED REMOVED - Admin is now detected by email during registration
            
            // APP SETTINGS
            dao.saveSettings(
                AppSetting(
                    id = 1,
                    taxRate = 0.00,
                    barName = "MidTown POS",
                    isDarkMode = true,
                    specialsJson = "{}"
                )
            )
            
            // MENU GROUPS
            dao.insertMenuGroup(MenuGroup(id = 1, name = "Liquor", sortOrder = 1))
            dao.insertMenuGroup(MenuGroup(id = 2, name = "Beer", sortOrder = 2))
            dao.insertMenuGroup(MenuGroup(id = 3, name = "Wine", sortOrder = 3))
            dao.insertMenuGroup(MenuGroup(id = 4, name = "Food", sortOrder = 4))
            dao.insertMenuGroup(MenuGroup(id = 5, name = "Shot Wall", sortOrder = 0))

            // CATEGORIES
            dao.insertCategory(Category(id = 1, menuGroupId = 1, name = "Vodka", iconName = "cocktail", displayOrder = 1))
            dao.insertCategory(Category(id = 2, menuGroupId = 1, name = "Whiskey", iconName = "cocktail", displayOrder = 2))
            dao.insertCategory(Category(id = 3, menuGroupId = 1, name = "Tequila", iconName = "cocktail", displayOrder = 3))
            dao.insertCategory(Category(id = 4, menuGroupId = 1, name = "Rum", iconName = "cocktail", displayOrder = 4))
            dao.insertCategory(Category(id = 5, menuGroupId = 2, name = "Beer Menu", iconName = "beer", displayOrder = 1))
            dao.insertCategory(Category(id = 8, menuGroupId = 3, name = "Wine", iconName = "wine", displayOrder = 1))
            dao.insertCategory(Category(id = 10, menuGroupId = 4, name = "Food", iconName = "food", displayOrder = 1))
            dao.insertCategory(Category(id = 12, menuGroupId = 5, name = "Shot Wall", iconName = "shot", displayOrder = 1))

            // MENU ITEMS - VODKA
            listOf("Tito's", "Grey Goose", "Kettle One", "Deep Eddy Lemon", "Deep Eddy Ruby", "Well Vodka").forEach {
                dao.insertMenuItem(MenuItem(categoryId = 1, name = it, price = 0.0))
            }

            // MENU ITEMS - WHISKEY
            listOf("Jack Daniels", "Jameson", "Crown Royal", "Makers Mark", "Jim Beam", "Well Whiskey").forEach {
                dao.insertMenuItem(MenuItem(categoryId = 2, name = it, price = 0.0))
            }

            // MENU ITEMS - TEQUILA
            listOf("Patron Silver", "Don Julio", "Espolon", "Jose Cuervo").forEach {
                dao.insertMenuItem(MenuItem(categoryId = 3, name = it, price = 0.0))
            }

            // MENU ITEMS - RUM
            listOf("Captain Morgan", "Bacardi", "Malibu").forEach {
                dao.insertMenuItem(MenuItem(categoryId = 4, name = it, price = 0.0))
            }

            // MENU ITEMS - BEER
            listOf(
                "Bud Light", "Miller Lite", "Coors Light", "Mich Ultra", "Busch Light",
                "Corona", "Modelo", "Heineken", "Stella",
                "Bud Light Draft", "Blue Moon Draft", "Guinness"
            ).forEach {
                dao.insertMenuItem(MenuItem(categoryId = 5, name = it, price = 0.0))
            }

            // MENU ITEMS - WINE
            listOf("House Cabernet", "House Merlot", "House Chard", "Pinot Grigio").forEach {
                dao.insertMenuItem(MenuItem(categoryId = 8, name = it, price = 0.0))
            }

            // MENU ITEMS - FOOD
            listOf("Wings (6)", "Wings (12)", "Nachos", "Pretzel Bites", "Mozzarella Sticks", "Classic Burger", "Cheeseburger").forEach {
                dao.insertMenuItem(MenuItem(categoryId = 10, name = it, price = 0.0))
            }

            // MENU ITEMS - SHOT WALL
            listOf(
                "Green Tea", "White Tea", "Lemon Drop", "Kamikaze", "Fireball", 
                "Jager Bomb", "Washington Apple", "Vegas Bomb", "House Tequila"
            ).forEach {
                dao.insertMenuItem(MenuItem(categoryId = 12, name = it, price = 0.0, isShotWallItem = true, inventoryCount = 999))
            }
        }
    }
}
