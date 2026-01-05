package com.finance.app.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.IncomeExpenseData
import com.finance.app.domain.model.TrendPoint
import com.finance.app.ui.accessibility.createDataVisualizationContentDescription
import com.finance.app.util.CurrencyUtils

/**
 * Analytics screen displaying financial reports and charts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Analytics",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (!uiState.hasData) {
            // Show time period selector even when there's no data
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DateRangeSelector(
                    selectedPreset = uiState.selectedPreset,
                    onPresetSelected = viewModel::setDateRangePreset
                )
                EmptyAnalyticsState(
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            AnalyticsContent(
                uiState = uiState,
                onDateRangePresetSelected = viewModel::setDateRangePreset,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

/**
 * Main analytics content with charts and statistics
 */
@Composable
private fun AnalyticsContent(
    uiState: AnalyticsUiState,
    onDateRangePresetSelected: (DateRangePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Range Selector
        DateRangeSelector(
            selectedPreset = uiState.selectedPreset,
            onPresetSelected = onDateRangePresetSelected
        )

        // Summary Statistics Cards
        SummaryStatistics(
            incomeExpenseData = uiState.incomeExpenseData,
            monthlyBalance = uiState.monthlyBalance
        )

        // Spending by Category Pie Chart
        if (uiState.spendingByCategory.isNotEmpty()) {
            SpendingByCategoryChart(
                spendingByCategory = uiState.spendingByCategory
            )
        }

        // Income vs Expense Bar Chart
        if (uiState.incomeExpenseData.totalIncome > 0 || uiState.incomeExpenseData.totalExpense > 0) {
            IncomeVsExpenseChart(
                incomeExpenseData = uiState.incomeExpenseData
            )
        }

        // Spending Trend Line Chart
        if (uiState.spendingTrend.isNotEmpty()) {
            SpendingTrendChart(
                spendingTrend = uiState.spendingTrend
            )
        }
    }
}

/**
 * Date range selector with preset options - all options visible
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DateRangeSelector(
    selectedPreset: DateRangePreset,
    onPresetSelected: (DateRangePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Date range selector",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Time Period",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Use FlowRow to wrap chips and show all options
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateRangePreset.values().forEach { preset ->
                    FilterChip(
                        selected = selectedPreset == preset,
                        onClick = { onPresetSelected(preset) },
                        label = { Text(preset.displayName) },
                        modifier = Modifier.semantics {
                            contentDescription = if (selectedPreset == preset) {
                                "Selected time period: ${preset.displayName}"
                            } else {
                                "Select time period: ${preset.displayName}"
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Summary statistics cards showing key financial metrics
 */
@Composable
private fun SummaryStatistics(
    incomeExpenseData: IncomeExpenseData,
    monthlyBalance: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Summary",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Income Card
            StatisticCard(
                title = "Total Income",
                value = CurrencyUtils.formatAmount(incomeExpenseData.totalIncome),
                icon = Icons.Default.TrendingUp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            // Total Expenses Card
            StatisticCard(
                title = "Total Expenses",
                value = CurrencyUtils.formatAmount(incomeExpenseData.totalExpense),
                icon = Icons.Default.TrendingDown,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }

        // Net Balance Card
        StatisticCard(
            title = "Net Balance",
            value = CurrencyUtils.formatAmount(incomeExpenseData.netBalance),
            icon = if (incomeExpenseData.netBalance >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
            color = if (incomeExpenseData.netBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Individual statistic card component with accessibility support
 */
@Composable
private fun StatisticCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    val contentDesc = "$title: $value"
    
    Card(
        modifier = modifier.semantics {
            contentDescription = contentDesc
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null, // Decorative, content description is on the card
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Pie chart showing spending breakdown by category with accessibility support
 */
@Composable
private fun SpendingByCategoryChart(
    spendingByCategory: Map<Category, Double>,
    modifier: Modifier = Modifier
) {
    // Create accessibility description for the chart
    val sortedEntries = spendingByCategory.entries.sortedByDescending { it.value }
    val chartDescription = createDataVisualizationContentDescription(
        chartType = "Spending by category chart",
        title = "Spending breakdown",
        summary = "Shows spending distribution across ${sortedEntries.size} categories",
        dataCount = sortedEntries.size
    )
    
    // Create detailed data breakdown for screen readers
    val detailedDataDescription = buildString {
        append("Detailed breakdown: ")
        sortedEntries.forEachIndexed { index, (category, amount) ->
            if (index > 0) append(", ")
            val percentage = (amount / spendingByCategory.values.sum() * 100).toInt()
            append("${category.name}: ${CurrencyUtils.formatAmount(amount)}, $percentage percent")
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Spending by Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (spendingByCategory.isNotEmpty()) {
                // Create chart entries
                val entries = spendingByCategory.entries.mapIndexed { index, (category, amount) ->
                    FloatEntry(index.toFloat(), amount.toFloat())
                }

                ProvideChartStyle {
                    Chart(
                        chart = columnChart(),
                        model = ChartEntryModelProducer(entries).requireModel(),
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .semantics {
                                contentDescription = chartDescription
                            }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Accessible data table for screen readers
                Column(
                    modifier = Modifier.semantics {
                        contentDescription = detailedDataDescription
                    }
                ) {
                    Text(
                        text = "Data breakdown:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Legend with enhanced accessibility
                    CategoryLegend(spendingByCategory = spendingByCategory)
                }
            }
        }
    }
}

/**
 * Legend for the pie chart showing categories and amounts with accessibility support
 */
@Composable
private fun CategoryLegend(
    spendingByCategory: Map<Category, Double>,
    modifier: Modifier = Modifier
) {
    val totalAmount = spendingByCategory.values.sum()
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        spendingByCategory.entries.sortedByDescending { it.value }.forEach { (category, amount) ->
            val percentage = if (totalAmount > 0) (amount / totalAmount * 100).toInt() else 0
            val itemDescription = "${category.name}: ${CurrencyUtils.formatAmount(amount)}, $percentage percent of total spending"
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = itemDescription
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(android.graphics.Color.parseColor(category.color)))
                        .semantics {
                            contentDescription = "Color indicator for ${category.name}"
                        }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = CurrencyUtils.formatAmount(amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Bar chart comparing income vs expenses with accessibility support
 */
@Composable
private fun IncomeVsExpenseChart(
    incomeExpenseData: IncomeExpenseData,
    modifier: Modifier = Modifier
) {
    val chartDescription = createDataVisualizationContentDescription(
        chartType = "Income versus expenses bar chart",
        title = "Financial comparison",
        summary = "Income: ${CurrencyUtils.formatAmount(incomeExpenseData.totalIncome)}, Expenses: ${CurrencyUtils.formatAmount(incomeExpenseData.totalExpense)}, Net: ${CurrencyUtils.formatAmount(incomeExpenseData.netBalance)}",
        dataCount = 2
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Income vs Expenses",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            val incomeEntries = listOf(FloatEntry(0f, incomeExpenseData.totalIncome.toFloat()))
            val expenseEntries = listOf(FloatEntry(1f, incomeExpenseData.totalExpense.toFloat()))

            val allEntries = incomeEntries + expenseEntries
            
            ProvideChartStyle {
                Chart(
                    chart = columnChart(),
                    model = ChartEntryModelProducer(allEntries).requireModel(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .semantics {
                            contentDescription = chartDescription
                        }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Accessible data summary
            Column(
                modifier = Modifier.semantics {
                    contentDescription = "Income and expense comparison data"
                }
            ) {
                Text(
                    text = "Financial Summary:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Legend for Income vs Expense with enhanced accessibility
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(
                        color = MaterialTheme.colorScheme.primary,
                        label = "Income",
                        amount = CurrencyUtils.formatAmount(incomeExpenseData.totalIncome),
                        modifier = Modifier.weight(1f)
                    )
                    LegendItem(
                        color = MaterialTheme.colorScheme.error,
                        label = "Expenses",
                        amount = CurrencyUtils.formatAmount(incomeExpenseData.totalExpense),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Net balance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Net balance: ${CurrencyUtils.formatAmount(incomeExpenseData.netBalance)}, ${if (incomeExpenseData.netBalance >= 0) "positive" else "negative"} balance"
                        },
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Net Balance: ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = CurrencyUtils.formatAmount(incomeExpenseData.netBalance),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (incomeExpenseData.netBalance >= 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
    }
}

/**
 * Line chart showing spending trends over time with accessibility support
 */
@Composable
private fun SpendingTrendChart(
    spendingTrend: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    val chartDescription = if (spendingTrend.isNotEmpty()) {
        val minAmount = spendingTrend.minOfOrNull { it.amount } ?: 0.0
        val maxAmount = spendingTrend.maxOfOrNull { it.amount } ?: 0.0
        val avgAmount = spendingTrend.map { it.amount }.average()
        
        createDataVisualizationContentDescription(
            chartType = "Spending trend line chart",
            title = "Spending over time",
            summary = "Range from ${CurrencyUtils.formatAmount(minAmount)} to ${CurrencyUtils.formatAmount(maxAmount)}, average ${CurrencyUtils.formatAmount(avgAmount)}",
            dataCount = spendingTrend.size
        )
    } else {
        "Spending trend chart with no data available"
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Spending Trend",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (spendingTrend.isNotEmpty()) {
                val entries = spendingTrend.mapIndexed { index, trendPoint ->
                    FloatEntry(index.toFloat(), trendPoint.amount.toFloat())
                }

                ProvideChartStyle {
                    Chart(
                        chart = lineChart(),
                        model = ChartEntryModelProducer(entries).requireModel(),
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .semantics {
                                contentDescription = chartDescription
                            }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Trend summary for accessibility
                val minAmount = spendingTrend.minOfOrNull { it.amount } ?: 0.0
                val maxAmount = spendingTrend.maxOfOrNull { it.amount } ?: 0.0
                val avgAmount = spendingTrend.map { it.amount }.average()
                
                Column(
                    modifier = Modifier.semantics {
                        contentDescription = "Trend analysis: Minimum spending ${CurrencyUtils.formatAmount(minAmount)}, Maximum spending ${CurrencyUtils.formatAmount(maxAmount)}, Average spending ${CurrencyUtils.formatAmount(avgAmount)}"
                    }
                ) {
                    Text(
                        text = "Trend Analysis:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Min: ${CurrencyUtils.formatAmount(minAmount)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Avg: ${CurrencyUtils.formatAmount(avgAmount)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Max: ${CurrencyUtils.formatAmount(maxAmount)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * Legend item component with accessibility support
 */
@Composable
private fun LegendItem(
    color: Color,
    label: String,
    amount: String? = null,
    modifier: Modifier = Modifier
) {
    val contentDesc = buildString {
        append("$label indicator")
        amount?.let { append(": $it") }
    }
    
    Row(
        modifier = modifier.semantics {
            contentDescription = contentDesc
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            amount?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Empty state when no analytics data is available with accessibility support
 */
@Composable
private fun EmptyAnalyticsState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = "No analytics data available",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Data Available",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start adding transactions to see your financial analytics",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
