package com.finance.app.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.app.domain.model.BudgetUtilization
import com.finance.app.ui.common.UiState
import com.finance.app.ui.components.BudgetProgressBar
import com.finance.app.ui.components.ErrorDisplay
import com.finance.app.ui.components.FinanceEmptyState
import com.finance.app.util.CurrencyUtils
import java.time.Month
import java.time.YearMonth

/**
 * Screen for displaying budget utilization analytics.
 * 
 * Shows budget progress bars for all categories with budgets, including:
 * - Visual progress indicators with color coding
 * - Spent vs budget amounts
 * - Overall utilization statistics
 * - Month navigation
 * 
 * @param onNavigateBack Callback when back button is pressed
 * @param viewModel The ViewModel managing budget analytics state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetUtilizationScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetAnalyticsViewModel = hiltViewModel()
) {
    val budgetUtilizationState by viewModel.budgetUtilizationState.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val currentYear by viewModel.currentYear.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Utilization") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (budgetUtilizationState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is UiState.Error -> {
                    ErrorDisplay(
                        error = budgetUtilizationState as UiState.Error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is UiState.Success -> {
                    val utilizations = (budgetUtilizationState as UiState.Success<List<BudgetUtilization>>).data
                    
                    if (utilizations.isEmpty()) {
                        FinanceEmptyState(
                            title = "No Budgets",
                            description = "Create a budget to track your spending",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            item {
                                // Month navigation
                                MonthNavigationHeader(
                                    month = currentMonth,
                                    year = currentYear,
                                    onPreviousMonth = { viewModel.previousMonth() },
                                    onNextMonth = { viewModel.nextMonth() },
                                    onResetToCurrentMonth = { viewModel.resetToCurrentMonth() }
                                )
                            }

                            item {
                                // Overall statistics
                                OverallStatisticsCard(
                                    utilizations = utilizations,
                                    viewModel = viewModel
                                )
                            }

                            item {
                                Text(
                                    text = "Category Breakdown",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                            }

                            items(utilizations) { utilization ->
                                BudgetProgressBar(utilization = utilization)
                            }

                            item {
                                // Bottom padding
                                Box(modifier = Modifier.padding(bottom = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Month navigation header component
 */
@Composable
private fun MonthNavigationHeader(
    month: Int,
    year: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToCurrentMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous month"
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${Month.of(month).name} $year",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next month"
            )
        }
    }
}

/**
 * Overall statistics card component
 */
@Composable
private fun OverallStatisticsCard(
    utilizations: List<BudgetUtilization>,
    viewModel: BudgetAnalyticsViewModel
) {
    val totalBudget = viewModel.getTotalBudget(utilizations)
    val totalSpending = viewModel.getTotalSpending(utilizations)
    val overallPercentage = viewModel.getOverallUtilizationPercentage(utilizations)
    val overBudgetCount = viewModel.getOverBudgetCount(utilizations)
    val criticalCount = viewModel.getCriticalCount(utilizations)
    val warningCount = viewModel.getWarningCount(utilizations)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Overall Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Budget",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtils.formatAmount(totalBudget),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {
                    Text(
                        text = "Total Spending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtils.formatAmount(totalSpending),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {
                    Text(
                        text = "Overall Usage",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.1f", overallPercentage)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Alert summary
            if (overBudgetCount > 0 || criticalCount > 0 || warningCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (overBudgetCount > 0) {
                        AlertBadge(
                            label = "Over Budget",
                            count = overBudgetCount,
                            color = androidx.compose.ui.graphics.Color(0xFFF44336)
                        )
                    }

                    if (criticalCount > 0) {
                        AlertBadge(
                            label = "Critical",
                            count = criticalCount,
                            color = androidx.compose.ui.graphics.Color(0xFFFF9800)
                        )
                    }

                    if (warningCount > 0) {
                        AlertBadge(
                            label = "Warning",
                            count = warningCount,
                            color = androidx.compose.ui.graphics.Color(0xFFFFC107)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Alert badge component for displaying alert counts
 */
@Composable
private fun AlertBadge(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Text(
            text = "$label: $count",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
