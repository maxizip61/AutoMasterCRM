package ru.automaster.app.di

import android.content.Context
import androidx.room.Room
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
    @Provides fun provideOrderEmployeeAssignmentDao(db: AppDatabase): OrderEmployeeAssignmentDao = db.orderEmployeeAssignmentDao()
    @Provides fun provideSalaryPaymentDao(db: AppDatabase): SalaryPaymentDao = db.salaryPaymentDao()
    @Provides fun provideTaxPaymentDao(db: AppDatabase): TaxPaymentDao = db.taxPaymentDao()
    @Provides fun provideStatusHistoryDao(db: AppDatabase): StatusHistoryDao = db.statusHistoryDao()
    @Provides fun providePhotoDao(db: AppDatabase): PhotoDao = db.photoDao()
    @Provides fun provideWorkScheduleDao(db: AppDatabase): WorkScheduleDao = db.workScheduleDao()
    @Provides fun provideFileDeletionTaskDao(db: AppDatabase): FileDeletionTaskDao = db.fileDeletionTaskDao()
    @Provides fun provideOrderWorkDao(db: AppDatabase): OrderWorkDao = db.orderWorkDao()

    // ---------- НОВЫЙ: провайдер для обучающего слоя OCR ----------
    @Provides fun provideOcrCorrectionDao(db: AppDatabase): OcrCorrectionDao = db.ocrCorrectionDao()
}