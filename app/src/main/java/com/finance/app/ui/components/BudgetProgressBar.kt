package com.finance.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finance.app.domain.model.AlertLevel
import com.finance.app.domain.model.BudgetUtilization
import com.finance.app.util.CurrencyUtils

/**
 * Composable for displaying a budget progress bar with color coding based on utilization level.
 * 
 * Shows visual representation of budget usage with appropriate color indicators:
 * - Green for normal usage (< 75%)
 * - Yellow for warning (75-89%)
 * - Orange for critical (90-99%)
 * - Red for over-budget (100%+)
 * 
 * @param utilization The BudgetUtilization data to display
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun BudgetProgressBar(
    utilization: BudgetUtilization,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Category name and percentage
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = utilization.categoryName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = "${String.format("%.1f", utilization.utilizationPercentage)}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = getAlertColor(utilization.getAlertLevel())
            )
        }

        // Progress bar background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            // Progress bar fill
            val fillPercentage = minOf(utilization.utilizationPercentage / 100.0, 1.0)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillPercentage.toFloat())
                    .height(8.dp)
                    .background(
                        color = getAlertColor(utilization.getAlertLevel()),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }

        // Spending details
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Spent: ${CurrencyUtils.formatAmount(utilization.currentSpending)}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp
            )
            
            Text(
                text = " / ",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            
            Text(
                text = "Budget: ${CurrencyUtils.formatAmount(utilization.budgetLimit)}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp
            )
            
            if (utilization.remainingAmount > 0.0) {
                Text(
                    text = " (${CurrencyUtils.formatAmount(utilization.remainingAmount)} left)",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Get the appropriate color for the alert level
 * 
 * @param alertLevel The alert level to get color for
 * @return Color corresponding to the alert level
 */
@Composable
fun getAlertColor(alertLevel: AlertLevel): Color {
    return when (alertLevel) {
        AlertLevel.NORMAL -> Color(0xFF4CAF50) // Green
        AlertLevel.WARNING -> Color(0xFFFFC107) // Yellow/Amber
        AlertLevel.CRITICAL -> Color(0xFFFF9800) // Orange
        AlertLevel.OVER_BUDGET -> Color(0xFFF44336) // Red
    }
}

/**
 * Composable for displaying a compact budget progress bar (minimal version)
 * 
 * @param utilization The BudgetUtilization data to display
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun CompactBudgetProgressBar(
    utilization: BudgetUtilization,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = utilization.categoryName,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .weight(2f)
                .height(6.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(3.dp)
                )
                .padding(horizontal = 4.dp)
        ) {
            val fillPercentage = minOf(utilization.utilizationPercentage / 100.0, 1.0)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillPercentage.toFloat())
                    .height(6.dp)
                    .background(
                        color = getAlertColor(utilization.getAlertLevel()),
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }

        Text(
            text = "${String.format("%.0f", utilization.utilizationPercentage)}%",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 8.dp),
            fontSize = 10.sp
        )
    }
}
