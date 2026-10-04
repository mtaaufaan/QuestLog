package com.rds.questlog

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.rds.questlog.data.billing.CurrentActivityHolder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class QuestLogApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var activityHolder: CurrentActivityHolder

    override fun onCreate() {
        super.onCreate()
        // Activity yang sedang tampil dibutuhkan alur pembelian Google Play (launchBillingFlow).
        registerActivityLifecycleCallbacks(activityHolder)
    }

    // WorkManager diinisialisasi manual (initializer bawaan dimatikan di manifest) agar worker bisa di-inject Hilt.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
