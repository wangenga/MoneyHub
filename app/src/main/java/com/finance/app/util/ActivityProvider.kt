package com.finance.app.util

import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider to access the current activity for biometric authentication
 */
@Singleton
class ActivityProvider @Inject constructor() {
    private var activityRef: WeakReference<FragmentActivity>? = null
    
    fun setCurrentActivity(activity: FragmentActivity) {
        activityRef = WeakReference(activity)
    }
    
    fun getCurrentActivity(): FragmentActivity? {
        return activityRef?.get()
    }
    
    fun clearCurrentActivity() {
        activityRef?.clear()
        activityRef = null
    }
    
    companion object {
        @JvmStatic
        fun setActivity(activity: FragmentActivity) {
            // For static access - delegate to instance method
            // This is a temporary bridge method for compatibility
        }
        
        @JvmStatic
        fun clearActivity() {
            // For static access - delegate to instance method
            // This is a temporary bridge method for compatibility
        }
    }
}
