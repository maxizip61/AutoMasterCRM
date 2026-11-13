package ru.automaster.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.automaster.app.data.local.AppDatabase
import ru.automaster.app.data.local.DatabaseSeeder
import ru.automaster.app.worker.WorkScheduler
import javax.inject.Inject

@HiltAndroidApp
class AutoMasterApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * SupervisorJob: падение одной задачи не отменяет остальные.
     * Dispatchers.IO: не блокируем main-поток при работе с БД.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Досоздаём справочники в уже существующей базе (лечит пустые posts и expense_categories)
        appScope.launch {
            DatabaseSeeder(AppDatabase.getDatabase(this@AutoMasterApp)).seedIfEmpty()
        }

        // Запускаем ежедневную очистку и бэкап
        WorkScheduler.scheduleDailyCleanup(this)
        WorkScheduler.scheduleAutoBackup(this)
    }
}