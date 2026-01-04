package com.finance.app.ui.accessibility

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finance.app.ui.components.*
import com.finance.app.util.CurrencyUtils

/**
 * Test screen for accessibility features and font scaling
 * This screen helps verify that all UI elements scale properly with system font size
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityTestScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    // Get current font scale
    val fontScale = density.fontScale
    val isLargeFontScale = fontScale >= 1.5f
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accessibility Test") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics {
                            contentDescription = "Navigate back to previous screen"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null // IconButton already has content description
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Font Scale Information
            AccessibilityInfoCard(
                title = "Font Scale Information",
                fontScale = fontScale,
                isLargeFontScale = isLargeFontScale
            )
            
            // Typography Samples
            TypographySamples()
            
            // Button Samples
            ButtonSamples()
            
            // Card Samples
            CardSamples()
            
            // Financial Data Samples
            FinancialDataSamples()
            
            // Interactive Elements
            InteractiveElementsSamples()
            
            // Accessibility Announcements Test
            AccessibilityAnnouncementTest()
        }
    }
}

@Composable
private fun AccessibilityInfoCard(
    title: String,
    fontScale: Float,
    isLargeFontScale: Boolean
) {
    FinanceCard(
        modifier = Modifier.semantics {
            contentDescription = "Font scale information: Current scale is $fontScale, ${if (isLargeFontScale) "large font scale detected" else "normal font scale"}"
        }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Current font scale: ${String.format("%.1f", fontScale)}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = if (isLargeFontScale) "Large font scale detected" else "Normal font scale",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isLargeFontScale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isLargeFontScale) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "UI elements should adapt to larger text sizes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TypographySamples() {
    FinanceCard(
        modifier = Modifier.semantics {
            contentDescription = "Typography samples showing different text sizes and styles"
        }
    ) {
        Text(
            text = "Typography Samples",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        val typographyStyles = listOf(
            "Display Large" to MaterialTheme.typography.displayLarge,
            "Headline Large" to MaterialTheme.typography.headlineLarge,
            "Headline Medium" to MaterialTheme.typography.headlineMedium,
            "Title Large" to MaterialTheme.typography.titleLarge,
            "Title Medium" to MaterialTheme.typography.titleMedium,
            "Body Large" to MaterialTheme.typography.bodyLarge,
            "Body Medium" to MaterialTheme.typography.bodyMedium,
            "Body Small" to MaterialTheme.typography.bodySmall,
            "Label Large" to MaterialTheme.typography.labelLarge,
            "Label Medium" to MaterialTheme.typography.labelMedium,
            "Label Small" to MaterialTheme.typography.labelSmall
        )
        
        typographyStyles.forEach { (name, style) ->
            Text(
                text = "$name - Sample Text",
                style = style,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ButtonSamples() {
    FinanceCard(
        modifier = Modifier.semantics {
            contentDescription = "Button samples with different styles and minimum touch targets"
        }
    ) {
        Text(
            text = "Button Samples (48dp minimum touch target)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FinancePrimaryButton(
                onClick = { },
                modifier = Modifier.semantics {
                    contentDescription = "Primary button sample"
                }
            ) {
                ButtonText("Primary Button")
            }
            
            FinanceSecondaryButton(
                onClick = { },
                modifier = Modifier.semantics {
                    contentDescription = "Secondary button sample"
                }
            ) {
                ButtonText("Secondary Button")
            }
            
            FinanceOutlinedButton(
                onClick = { },
                modifier = Modifier.semantics {
                    contentDescription = "Outlined button sample"
                }
            ) {
                ButtonText("Outlined Button")
            }
            
            FinanceTextButton(
                onClick = { },
                modifier = Modifier.semantics {
                    contentDescription = "Text button sample"
                }
            ) {
                ButtonText("Text Button")
            }
        }
    }
}

@Composable
private fun CardSamples() {
    FinanceCard(
        modifier = Modifier.semantics {
            contentDescription = "Card samples showing different card types"
        }
    ) {
        Text(
            text = "Card Samples",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FinanceSummaryCard(
                title = "Sample Metric",
                value = CurrencyUtils.formatAmount(1234.56),
                subtitle = "This month",
                onClick = { }
            )
            
            FinanceElevatedCard(
                modifier = Modifier.semantics {
                    contentDescription = "Elevated card sample with important information"
                }
            ) {
                Text(
                    text = "Elevated Card",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "This card has higher elevation for emphasis",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun FinancialDataSamples() {
    FinanceCard(
        modifier = Modifier.semantics {
            contentDescription = "Financial data samples showing currency formatting and transaction information"
        }
    ) {
        Text(
            text = "Financial Data Samples",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Sample transaction-like item
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sample Transaction",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Food & Dining • Today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = CurrencyUtils.formatAmount(-25.50),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Divider()
            
            // Sample balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Balance",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = CurrencyUtils.formatAmount(1234.56),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun InteractiveElementsSamples() {
    var switchState by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableStateOf(0.5f) }
    
    FinanceCard(
        modifier = Modifier.semantics {
            contentDescription = "Interactive elements samples including switches, sliders, and checkboxes"
        }
    ) {
        Text(
            text = "Interactive Elements",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sample Setting",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = switchState,
                    onCheckedChange = { switchState = it },
                    modifier = Modifier.semantics {
                        contentDescription = "Sample setting toggle, currently ${if (switchState) "enabled" else "disabled"}"
                    }
                )
            }
            
            // Slider
            Column {
                Text(
                    text = "Sample Slider: ${(sliderValue * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    modifier = Modifier.semantics {
                        contentDescription = "Sample slider, current value ${(sliderValue * 100).toInt()} percent"
                    }
                )
            }
            
            // Icon Button
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { },
                    modifier = Modifier.semantics {
                        contentDescription = "Sample action button"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null // IconButton already has content description
                    )
                }
                Text(
                    text = "Icon Button (48dp touch target)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun AccessibilityAnnouncementTest() {
    var announcementCount by remember { mutableStateOf(0) }
    
    FinanceCard(
        modifier = Modifier.semantics {
            contentDescription = "Accessibility announcement test for screen reader feedback"
        }
    ) {
        Text(
            text = "Accessibility Announcements",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Test accessibility announcements for screen readers",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        FinancePrimaryButton(
            onClick = { announcementCount++ },
            modifier = Modifier.semantics {
                contentDescription = "Test announcement button, pressed $announcementCount times"
            }
        ) {
            ButtonText("Test Announcement ($announcementCount)")
        }
        
        // Accessibility announcement
        AccessibilityAnnouncement(
            message = "Button pressed $announcementCount times",
            shouldAnnounce = announcementCount > 0
        )
    }
}