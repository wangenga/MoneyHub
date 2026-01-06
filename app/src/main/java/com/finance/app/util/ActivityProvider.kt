package com.finance.app.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider to access the current activity for biometric authentication.
 * Uses Application.ActivityLifecycleCallbacks to automatically track the current activity.
 */
@Singleton
class ActivityProvider @Inject constructor() : Application.ActivityLifecycleCallbacks {
    
    private var currentActivityRef: WeakReference<FragmentActivity>? = null
    
    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
        Log.d("ActivityProvider", "Registered activity lifecycle callbacks")
    }
    
    fun setCurrentActivity(activity: FragmentActivity) {
        currentActivityRef = WeakReference(activity)
        Log.d("ActivityProvider", "Activity set: ${activity::class.simpleName}")
    }
    
    fun getCurrentActivity(): FragmentActivity? {
        val activity = currentActivityRef?.get()
        Log.d("ActivityProvider", "getCurrentActivity called, activity: ${activity?.let { it::class.simpleName } ?: "null"}")
        return activity
    }
    
    fun clearCurrentActivity() {
        Log.d("ActivityProvider", "Clearing current activity")
        currentActivityRef?.clear()
        currentActivityRef = null
    }
    
    // ActivityLifecycleCallbacks implementation
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is FragmentActivity) {
            currentActivityRef = WeakReference(activity)
            Log.d("ActivityProvider", "onActivityCreated: ${activity::class.simpleName}")
        }
    }
    
    override fun onActivityStarted(activity: Activity) {}
    
    override fun onActivityResumed(activity: Activity) {
        if (activity is FragmentActivity) {
            currentActivityRef = WeakReference(activity)
            Log.d("ActivityProvider", "onActivityResumed: ${activity::class.simpleName}")
        }
    }
    
    override fun onActivityPaused(activity: Activity) {}
    
    override fun onActivityStopped(activity: Activity) {}
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivityRef?.get() === activity) {
            Log.d("ActivityProvider", "onActivityDestroyed: ${activity::class.simpleName}")
            currentActivityRef?.clear()
            currentActivityRef = null
        }
    }
    
    companion object {
        @JvmStatic
        fun setActivity(activity: FragmentActivity) {
            // Deprecated - use instance method instead
        }
        
        @JvmStatic
        fun clearActivity() {
            // Deprecated - use instance method instead
        }
    }
}
