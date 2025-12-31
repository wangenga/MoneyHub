package com.finance.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Circular loading indicator component
 */
@Composable
fun FinanceLoadingIndicator(
    modifier: Modifier = Modifier,
    size: LoadingSize = LoadingSize.Medium
) {
    CircularProgressIndicator(
        modifier = modifier.size(size.dp.dp),
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 3.dp
    )
}

/**
 * Linear loading indicator component
 */
@Composable
fun FinanceLinearLoadingIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    if (progress != null) {
        LinearProgressIndicator(
            progress = progress,
            modifier = modifier,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    } else {
        LinearProgressIndicator(
            modifier = modifier,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * Full screen loading indicator with optional message
 */
@Composable
fun FinanceFullScreenLoading(
    message: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FinanceLoadingIndicator(size = LoadingSize.Large)
            
            if (message != null) {
                FinanceBodyText(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

/**
 * Loading sizes for different use cases
 */
enum class LoadingSize(val dp: Int) {
    Small(16),
    Medium(24),
    Large(48),
    ExtraLarge(64)
}