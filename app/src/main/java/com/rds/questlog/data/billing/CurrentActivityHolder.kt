package com.rds.questlog.data.billing

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Activity yang sedang tampil, dibutuhkan BillingClient.launchBillingFlow. Disimpan sebagai WeakReference agar tidak
 * membocorkan Activity; didaftarkan sekali dari [com.rds.questlog.QuestLogApp].
 */
@Singleton
class CurrentActivityHolder @Inject constructor() : Application.ActivityLifecycleCallbacks {

    private var resumed: WeakReference<Activity>? = null

    val current: Activity? get() = resumed?.get()

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed?.get() === activity) resumed = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
