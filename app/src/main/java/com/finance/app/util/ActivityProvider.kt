package com.finance.app.util

import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference

/**
 * Singleton to provide access to the current activity for biometric authentication
 */
object ActivityProvider {
    private var activityRef: WeakReference<FragmentActivity>? = null
    
    fun setActivity(activity: FragmentActivity) {
        activityRef = WeakReference(activity)
    }
    
    fun getActivity(): FragmentActivity? {
        return activityRef?.get()
    }
    
    fun clearActivity() {
        activityRef?.clear()
        activityRef = null
    }
}
