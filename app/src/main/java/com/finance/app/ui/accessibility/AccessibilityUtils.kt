package com.finance.app.ui.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * Utility functions for accessibility support
 */
object AccessibilityUtils {
    
    /**
     * Announces a message to screen readers
     */
    fun announceForAccessibility(view: View, message: String) {
        view.announceForAccessibility(message)
    }
    
    /**
     * Checks if accessibility services are enabled
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return accessibilityManager.isEnabled
    }
    
    /**
     * Checks if TalkBack is enabled
     */
    fun isTalkBackEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return accessibilityManager.isEnabled && 
               accessibilityManager.isTouchExplorationEnabled
    }
    
    /**
     * Requests focus for accessibility
     */
    fun requestAccessibilityFocus(view: View) {
        view.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }
}

/**
 * Modifier for live regions that announce changes to screen readers
 */
fun Modifier.accessibilityLiveRegion(mode: LiveRegionMode = LiveRegionMode.Polite): Modifier {
    return this.semantics {
        liveRegion = mode
    }
}

/**
 * Composable that announces state changes to accessibility services
 */
@Composable
fun AccessibilityAnnouncement(
    message: String,
    shouldAnnounce: Boolean = true
) {
    val view = LocalView.current
    val context = LocalContext.current
    
    LaunchedEffect(message, shouldAnnounce) {
        if (shouldAnnounce && AccessibilityUtils.isAccessibilityEnabled(context)) {
            AccessibilityUtils.announceForAccessibility(view, message)
        }
    }
}

/**
 * Composable that manages focus for accessibility
 */
@Composable
fun AccessibilityFocusManager(
    shouldFocus: Boolean,
    focusMessage: String? = null
) {
    val view = LocalView.current
    val context = LocalContext.current
    
    LaunchedEffect(shouldFocus) {
        if (shouldFocus && AccessibilityUtils.isAccessibilityEnabled(context)) {
            AccessibilityUtils.requestAccessibilityFocus(view)
            focusMessage?.let { message ->
                AccessibilityUtils.announceForAccessibility(view, message)
            }
        }
    }
}

/**
 * Creates a comprehensive content description for financial data
 */
fun createFinancialContentDescription(
    title: String,
    amount: String,
    additionalInfo: String? = null
): String {
    return buildString {
        append(title)
        append(": ")
        append(amount)
        additionalInfo?.let {
            append(", ")
            append(it)
        }
    }
}

/**
 * Creates a content description for chart data
 */
fun createChartContentDescription(
    chartType: String,
    dataPoints: List<Pair<String, String>>,
    summary: String? = null
): String {
    return buildString {
        append("$chartType chart. ")
        summary?.let {
            append("$it. ")
        }
        append("Data points: ")
        dataPoints.forEachIndexed { index, (label, value) ->
            if (index > 0) append(", ")
            append("$label: $value")
        }
    }
}

/**
 * Creates a content description for transaction items
 */
fun createTransactionContentDescription(
    category: String,
    amount: String,
    date: String,
    notes: String? = null,
    actionHint: String = "Double tap to edit"
): String {
    return buildString {
        append("Transaction: ")
        append("$category, ")
        append("$amount, ")
        append(date)
        notes?.let {
            append(", Note: $it")
        }
        append(". $actionHint")
    }
}