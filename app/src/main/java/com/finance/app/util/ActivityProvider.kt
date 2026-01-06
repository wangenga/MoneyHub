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
        // Also update static reference for backward compatibility
        staticActivityRef = activityRef
    }
    
    fun getCurrentActivity(): FragmentActivity? {
        return activityRef?.get() ?: staticActivityRef?.get()
    }
    
    fun clearCurrentActivity() {
        activityRef?.clear()
        activityRef = null
        staticActivityRef?.clear()
        staticActivityRef = null
    }
    
    companion object {
        private var staticActivityRef: WeakReference<FragmentActivity>? = null
        
        @JvmStatic
        fun setActivity(activity: FragmentActivity) {
            staticActivityRef = WeakReference(activity)
        }
        
        @JvmStatic
        fun clearActivity() {
            staticActivityRef?.clear()
            staticActivityRef = null
        }
    }
}
