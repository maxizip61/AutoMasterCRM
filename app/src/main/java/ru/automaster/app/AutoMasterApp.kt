package ru.automaster.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import ru.automaster.app.worker.WorkScheduler
import javax.inject.Inject

@HiltAndroidApp
class AutoMasterApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Запускаем ежедневную очистку и бэкап
        WorkScheduler.scheduleDailyCleanup(this)
        WorkScheduler.scheduleAutoBackup(this)
    }
}