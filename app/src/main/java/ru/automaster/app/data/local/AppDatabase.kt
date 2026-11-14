package ru.automaster.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.automaster.app.BuildConfig
import ru.automaster.app.data.local.dao.*
import ru.automaster.app.data.local.entity.*

@Database(
    entities = [
        ClientEntity::class, CarEntity::class, OrderEntity::class, OrderStageEntity::class,
        PostEntity::class, PaymentEntity::class, ExpenseCategoryEntity::class, ExpenseEntity::class,
        MaterialUsageEntity::class, StatusHistoryEntity::class, PhotoEntity::class,
        FileDeletionTaskEntity::class, EmployeeEntity::class, OrderEmployeeAssignmentEntity::class,
        OrderEmployeeAccrualEntity::class, SalaryPaymentEntity::class, TaxPaymentEntity::class,
        WorkScheduleEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun clientDao(): ClientDao
    abstract fun carDao(): CarDao
    abstract fun orderDao(): OrderDao
    abstract fun orderStageDao(): OrderStageDao
    abstract fun postDao(): PostDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun materialUsageDao(): MaterialUsageDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun orderEmployeeAccrualDao(): OrderEmployeeAccrualDao
    abstract fun orderEmployeeAssignmentDao(): OrderEmployeeAssignmentDao
    abstract fun salaryPaymentDao(): SalaryPaymentDao
    abstract fun taxPaymentDao(): TaxPaymentDao
    abstract fun statusHistoryDao(): StatusHistoryDao
    abstract fun photoDao(): PhotoDao
    abstract fun workScheduleDao(): WorkScheduleDao
    abstract fun fileDeletionTaskDao(): FileDeletionTaskDao   // ← БЕЗ скобок ()

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val ALL_MIGRATIONS = arrayOf<Migration>(MIGRATION_1_2)

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "automaster_database"
                )
                    .addCallback(SeedCallback())
                    .addMigrations(*ALL_MIGRATIONS)
                    .apply { if (BuildConfig.DEBUG) fallbackToDestructiveMigration() }
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Seed через SupportSQLiteDatabase напрямую — INSTANCE ещё null в момент onCreate,
     * поэтому нельзя использовать DAO. ContentValues + CONFLICT_IGNORE делают seed идемпотентным.
     */
    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Посты
                    val posts = listOf(
                        Triple("PREP_1", "PREP", "Пост кузовных работ 1"),
                        Triple("PREP_2", "PREP", "Пост кузовных работ 2"),
                        Triple("PAINT_1", "PAINT", "Покрасочная камера")
                    )
                    posts.forEachIndexed { index, (name, type, display) ->
                        val cv = ContentValues().apply {
                            put("name", name)
                            put("type", type)
                            put("displayName", display)
                            put("isActive", 1)
                            put("sortOrder", index + 1)
                        }
                        db.insert("posts", SQLiteDatabase.CONFLICT_IGNORE, cv)
                    }

                    // Категории расходов
                    val categories = listOf(
                        "Аренда", "Коммунальные", "Налоги", "Оборудование",
                        "Инструменты", "Расходники", "Прочее"
                    )
                    categories.forEach { name ->
                        val cv = ContentValues().apply {
                            put("name", name)
                            put("defaultInclude", 1)
                        }
                        db.insert("expense_categories", SQLiteDatabase.CONFLICT_IGNORE, cv)
                    }
                    Log.i("SeedCallback", "Seed выполнен: 3 поста, 7 категорий")
                } catch (e: Exception) {
                    Log.e("SeedCallback", "Ошибка seed", e)
                }
            }
        }
    }
}