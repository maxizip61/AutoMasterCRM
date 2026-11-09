package ru.automaster.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.automaster.app.data.local.AppDatabase
import ru.automaster.app.data.local.dao.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "automaster_database")
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    db.execSQL("INSERT INTO posts (name, type) VALUES ('PREP_1', 'PREP'), ('PREP_2', 'PREP'), ('PAINT_1', 'PAINT')")
                    db.execSQL("INSERT INTO expense_categories (name, defaultInclude) VALUES ('Аренда', 1), ('Коммунальные', 1), ('Налоги', 1), ('Расходники', 1)")
                }
            })
            .build()
    }

    @Provides fun provideClientDao(db: AppDatabase): ClientDao = db.clientDao()
    @Provides fun provideCarDao(db: AppDatabase): CarDao = db.carDao()
    @Provides fun provideOrderDao(db: AppDatabase): OrderDao = db.orderDao()
    @Provides fun provideOrderStageDao(db: AppDatabase): OrderStageDao = db.orderStageDao()
    @Provides fun providePostDao(db: AppDatabase): PostDao = db.postDao()
    @Provides fun providePaymentDao(db: AppDatabase): PaymentDao = db.paymentDao()
    @Provides fun provideExpenseCategoryDao(db: AppDatabase): ExpenseCategoryDao = db.expenseCategoryDao()
    @Provides fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()
    @Provides fun provideMaterialUsageDao(db: AppDatabase): MaterialUsageDao = db.materialUsageDao()
    @Provides fun provideEmployeeDao(db: AppDatabase): EmployeeDao = db.employeeDao()
    @Provides fun provideOrderEmployeeAccrualDao(db: AppDatabase): OrderEmployeeAccrualDao = db.orderEmployeeAccrualDao()
    @Provides fun provideSalaryPaymentDao(db: AppDatabase): SalaryPaymentDao = db.salaryPaymentDao()
    @Provides fun provideTaxPaymentDao(db: AppDatabase): TaxPaymentDao = db.taxPaymentDao()

    // ВОТ ЭТИХ НЕ ХВАТАЛО:
    @Provides fun provideStatusHistoryDao(db: AppDatabase): StatusHistoryDao = db.statusHistoryDao()
    @Provides fun providePhotoDao(db: AppDatabase): PhotoDao = db.photoDao()
    @Provides fun provideWorkScheduleDao(db: AppDatabase): WorkScheduleDao = db.workScheduleDao()
    @Provides fun provideFileDeletionTaskDao(db: AppDatabase): FileDeletionTaskDao = db.fileDeletionTaskDao()
    @Provides fun provideOrderEmployeeAssignmentDao(db: AppDatabase): OrderEmployeeAssignmentDao = db.orderEmployeeAssignmentDao()
}