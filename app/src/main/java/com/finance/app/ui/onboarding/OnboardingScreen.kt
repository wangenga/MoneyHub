package com.finance.app.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

/**
 * Main onboarding screen with horizontal pager
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onNavigateToAuth: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { viewModel.pages.size })
    val coroutineScope = rememberCoroutineScope()
    
    // Handle navigation
    LaunchedEffect(uiState.shouldNavigateToAuth) {
        if (uiState.shouldNavigateToAuth) {
            onNavigateToAuth()
            viewModel.onNavigationHandled()
        }
    }
    
    // Sync pager state with ViewModel
    LaunchedEffect(uiState.currentPage) {
        pagerState.animateScrollToPage(uiState.currentPage)
    }
    
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != uiState.currentPage) {
            // Update ViewModel when user swipes
            repeat(pagerState.currentPage - uiState.currentPage) {
                viewModel.nextPage()
            }
            repeat(uiState.currentPage - pagerState.currentPage) {
                viewModel.previousPage()
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Skip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { viewModel.skipOnboarding() }
            ) {
                Text("Skip")
            }
        }
        
        // Pager content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingPageContent(
                page = viewModel.pages[page],
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Page indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            repeat(viewModel.pages.size) { index ->
                val isSelected = index == uiState.currentPage
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            if (uiState.currentPage > 0) {
                OutlinedButton(
                    onClick = { 
                        coroutineScope.launch {
                            viewModel.previousPage()
                        }
                    }
                ) {
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.width(80.dp))
            }
            
            // Next/Get Started button
            Button(
                onClick = {
                    if (uiState.currentPage < viewModel.pages.size - 1) {
                        coroutineScope.launch {
                            viewModel.nextPage()
                        }
                    } else {
                        viewModel.completeOnboarding()
                    }
                }
            ) {
                Text(
                    if (uiState.currentPage < viewModel.pages.size - 1) "Next" 
                    else "Get Started"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}