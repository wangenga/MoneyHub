package com.finance.app.domain.model

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data class representing an onboarding page
 */
data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector
)

/**
 * Predefined onboarding pages explaining key features
 */
object OnboardingPages {
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to Personal Finance",
            description = "Take control of your finances with our comprehensive money management app. Track expenses, manage budgets, and gain insights into your spending habits.",
            icon = Icons.Default.Wallet
        ),
        OnboardingPage(
            title = "Track Your Transactions",
            description = "Easily add income and expenses with detailed categorization. Never lose track of where your money goes with our intuitive transaction management.",
            icon = Icons.Default.Category
        ),
        OnboardingPage(
            title = "Insightful Analytics",
            description = "Visualize your spending patterns with beautiful charts and reports. Understand your financial trends and make informed decisions.",
            icon = Icons.Default.Analytics
        ),
        OnboardingPage(
            title = "Secure & Private",
            description = "Your financial data is protected with bank-level encryption and biometric authentication. Your privacy is our top priority.",
            icon = Icons.Default.Security
        ),
        OnboardingPage(
            title = "Sync Across Devices",
            description = "Access your financial data from anywhere with automatic cloud synchronization. Your data stays up-to-date across all your devices.",
            icon = Icons.Default.Sync
        )
    )
}