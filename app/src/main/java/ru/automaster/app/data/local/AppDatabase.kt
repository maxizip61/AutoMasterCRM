package ru.automaster.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
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
        WorkScheduleEntity::class, OrderWorkEntity::class
    ],
    version = 5,
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
    abstract fun fileDeletionTaskDao(): FileDeletionTaskDao
    abstract fun orderWorkDao(): OrderWorkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        val ALL_MIGRATIONS = arrayOf<Migration>(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5
        )

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "automaster_database"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .apply { if (BuildConfig.DEBUG) fallbackToDestructiveMigration() }
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}