@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.synfusion.attendance.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synfusion.attendance.R
import com.synfusion.attendance.ui.responsive.ProvideWindowSizeInfo
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.viewmodel.OfflineOnboardingViewModel
import com.synfusion.attendance.ui.onboarding.steps.WelcomeStep
import com.synfusion.attendance.ui.onboarding.steps.AttendanceStep
import com.synfusion.attendance.ui.onboarding.steps.EarningsStep
import com.synfusion.attendance.ui.onboarding.steps.PassbookStep
import com.synfusion.attendance.ui.onboarding.steps.FinalIntroStep

@Composable
fun OnboardingScreen(
    viewModel: OfflineOnboardingViewModel,
    onOnboardingComplete: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = viewModel.state.value)

    // Intercept hardware back button when on steps 1..4
    BackHandler(enabled = state.currentStep in 1..4) {
        viewModel.onPreviousStep()
    }

    ProvideWindowSizeInfo {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.width(48.dp)) {
                        if (state.currentStep > 0) {
                            IconButton(onClick = { viewModel.onPreviousStep() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.onboarding_back),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // 5-Step Progress Indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            val isActive = index <= state.currentStep
                            Box(
                                modifier = Modifier
                                    .width(if (index == state.currentStep) 24.dp else 12.dp)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(48.dp))
                }
            },
            contentWindowInsets = WindowInsets(0.dp)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                ResponsiveContainer {
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 5 })

                    // Sync pager state with view model state
                    LaunchedEffect(state.currentStep) {
                        if (pagerState.currentPage != state.currentStep) {
                            pagerState.animateScrollToPage(state.currentStep)
                        }
                    }
                    LaunchedEffect(pagerState.currentPage) {
                        if (pagerState.currentPage != state.currentStep && !pagerState.isScrollInProgress) {
                            viewModel.goToStep(pagerState.currentPage)
                        }
                    }

                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = true
                    ) { step ->
                        when (step) {
                            0 -> WelcomeStep(onNext = { viewModel.onNextStep() })
                            1 -> AttendanceStep(onNext = { viewModel.onNextStep() })
                            2 -> EarningsStep(onNext = { viewModel.onNextStep() })
                            3 -> PassbookStep(onNext = { viewModel.onNextStep() })
                            4 -> FinalIntroStep(
                                onComplete = {
                                    viewModel.completeOnboarding {
                                        onOnboardingComplete()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
