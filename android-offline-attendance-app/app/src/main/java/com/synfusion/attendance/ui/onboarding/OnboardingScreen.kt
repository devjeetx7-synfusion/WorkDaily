@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.synfusion.attendance.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.synfusion.attendance.ui.onboarding.steps.AttendanceStep
import com.synfusion.attendance.ui.onboarding.steps.EarningsStep
import com.synfusion.attendance.ui.onboarding.steps.FinalIntroStep
import com.synfusion.attendance.ui.onboarding.steps.PassbookStep
import com.synfusion.attendance.ui.onboarding.steps.WelcomeStep
import com.synfusion.attendance.ui.responsive.ProvideWindowSizeInfo
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.viewmodel.OfflineOnboardingViewModel

@Composable
fun OnboardingScreen(
    viewModel: OfflineOnboardingViewModel,
    onOnboardingComplete: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = viewModel.state.value)

    BackHandler(enabled = state.currentStep in 1..4) {
        viewModel.onPreviousStep()
    }

    ProvideWindowSizeInfo {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (index <= state.currentStep) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                            )
                        }
                    }
                }
            },
            contentWindowInsets = WindowInsets(0.dp)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
            ) {
                ResponsiveContainer {
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                        initialPage = state.currentStep,
                        pageCount = { 5 }
                    )

                    LaunchedEffect(state.currentStep) {
                        if (pagerState.currentPage != state.currentStep && !pagerState.isScrollInProgress) {
                            pagerState.animateScrollToPage(state.currentStep)
                        }
                    }

                    LaunchedEffect(pagerState.isScrollInProgress) {
                        if (!pagerState.isScrollInProgress && pagerState.currentPage != state.currentStep) {
                            viewModel.goToStep(pagerState.currentPage)
                        }
                    }

                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = true,
                        beyondViewportPageCount = 1
                    ) { step ->
                        when (step) {
                            0 -> WelcomeStep(
                                onNext = { viewModel.onNextStep() }
                            )

                            1 -> AttendanceStep(
                                onBack = { viewModel.onPreviousStep() },
                                onNext = { viewModel.onNextStep() }
                            )

                            2 -> EarningsStep(
                                onBack = { viewModel.onPreviousStep() },
                                onNext = { viewModel.onNextStep() }
                            )

                            3 -> PassbookStep(
                                onBack = { viewModel.onPreviousStep() },
                                onNext = { viewModel.onNextStep() }
                            )

                            4 -> FinalIntroStep(
                                onBack = { viewModel.onPreviousStep() },
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
