package com.finance.app.ui.accessibility

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
 * Composable for managing state change announcements
 */
@Composable
fun StateChangeAnnouncement(
    state: String,
    previousState: String? = null,
    shouldAnnounce: Boolean = true
) {
    val view = LocalView.current
    val context = LocalContext.current
    
    LaunchedEffect(state) {
        if (shouldAnnounce && 
            AccessibilityUtils.isAccessibilityEnabled(context) && 
            state != previousState) {
            val message = "State changed to $state"
            AccessibilityUtils.announceForAccessibility(view, message)
        }
    }
}

/**
 * Modifier for ensuring minimum touch target size
 */
fun Modifier.minimumTouchTarget(minSize: Int = 48): Modifier {
    return this.sizeIn(minWidth = minSize.dp, minHeight = minSize.dp)
}

/**
 * Modifier for adding semantic role and content description
 */
fun Modifier.accessibleClickable(
    contentDescription: String,
    role: androidx.compose.ui.semantics.Role = androidx.compose.ui.semantics.Role.Button,
    onClick: () -> Unit
): Modifier {
    return this
        .minimumTouchTarget()
        .clickable(
            onClickLabel = contentDescription,
            role = role,
            onClick = onClick
        )
        .semantics {
            this.contentDescription = contentDescription
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

/**
 * Creates a content description for form fields with validation state
 */
fun createFormFieldContentDescription(
    label: String,
    value: String,
    isError: Boolean = false,
    errorMessage: String? = null,
    isRequired: Boolean = false
): String {
    return buildString {
        append(label)
        if (isRequired) append(", required")
        if (value.isNotEmpty()) {
            append(", current value: $value")
        } else {
            append(", empty")
        }
        if (isError && errorMessage != null) {
            append(", error: $errorMessage")
        }
    }
}

/**
 * Creates a content description for toggle buttons
 */
fun createToggleContentDescription(
    label: String,
    isSelected: Boolean,
    groupContext: String? = null
): String {
    return buildString {
        if (groupContext != null) {
            append("$groupContext: ")
        }
        append(label)
        append(if (isSelected) ", selected" else ", not selected")
        append(". Double tap to ${if (isSelected) "deselect" else "select"}.")
    }
}

/**
 * Creates a content description for progress indicators
 */
fun createProgressContentDescription(
    operation: String,
    progress: Int? = null
): String {
    return buildString {
        append("$operation in progress")
        progress?.let {
            append(", $it percent complete")
        }
    }
}

/**
 * Creates a content description for navigation elements
 */
fun createNavigationContentDescription(
    destination: String,
    currentLocation: String? = null,
    hasNotification: Boolean = false
): String {
    return buildString {
        append("Navigate to $destination")
        currentLocation?.let {
            if (it.equals(destination, ignoreCase = true)) {
                append(", currently selected")
            }
        }
        if (hasNotification) {
            append(", has notifications")
        }
    }
}

/**
 * Creates a content description for data visualization elements
 */
fun createDataVisualizationContentDescription(
    chartType: String,
    title: String,
    summary: String,
    dataCount: Int
): String {
    return buildString {
        append("$chartType titled $title. ")
        append("$summary. ")
        append("Contains $dataCount data points. ")
        append("Swipe right for detailed data breakdown.")
    }
}