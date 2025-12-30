package com.finance.app.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.toArgb
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
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.composed.ComposedChartEntryModelProducer
import com.finance.app.domain.model.Category
import com.finance.app.domain.model.IncomeExpenseData
import com.finance.app.domain.model.TrendPoint
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

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
            EmptyAnalyticsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
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
 * Date range selector with preset options
 */
@OptIn(ExperimentalMaterial3Api::class)
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
                    contentDescription = null,
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
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DateRangePreset.values()) { preset ->
                    FilterChip(
                        selected = selectedPreset == preset,
                        onClick = { onPresetSelected(preset) },
                        label = { Text(preset.displayName) }
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
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = Currency.getInstance("USD")
        }
    }

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
                value = currencyFormatter.format(incomeExpenseData.totalIncome),
                icon = Icons.Default.TrendingUp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            // Total Expenses Card
            StatisticCard(
                title = "Total Expenses",
                value = currencyFormatter.format(incomeExpenseData.totalExpense),
                icon = Icons.Default.TrendingDown,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }

        // Net Balance Card
        StatisticCard(
            title = "Net Balance",
            value = currencyFormatter.format(incomeExpenseData.netBalance),
            icon = if (incomeExpenseData.netBalance >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
            color = if (incomeExpenseData.netBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Individual statistic card component
 */
@Composable
private fun StatisticCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
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
 * Pie chart showing spending breakdown by category
 */
@Composable
private fun SpendingByCategoryChart(
    spendingByCategory: Map<Category, Double>,
    modifier: Modifier = Modifier
) {
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
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Legend
                CategoryLegend(spendingByCategory = spendingByCategory)
            }
        }
    }
}

/**
 * Legend for the pie chart showing categories and amounts
 */
@Composable
private fun CategoryLegend(
    spendingByCategory: Map<Category, Double>,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = Currency.getInstance("USD")
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        spendingByCategory.entries.sortedByDescending { it.value }.forEach { (category, amount) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(android.graphics.Color.parseColor(category.color)))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = currencyFormatter.format(amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Bar chart comparing income vs expenses
 */
@Composable
private fun IncomeVsExpenseChart(
    incomeExpenseData: IncomeExpenseData,
    modifier: Modifier = Modifier
) {
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
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend for Income vs Expense
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(
                    color = MaterialTheme.colorScheme.primary,
                    label = "Income",
                    modifier = Modifier.weight(1f)
                )
                LegendItem(
                    color = MaterialTheme.colorScheme.error,
                    label = "Expenses",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Line chart showing spending trends over time
 */
@Composable
private fun SpendingTrendChart(
    spendingTrend: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
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
                    )
                }
            }
        }
    }
}

/**
 * Legend item component
 */
@Composable
private fun LegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
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
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Empty state when no analytics data is available
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
                contentDescription = null,
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
