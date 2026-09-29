@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.synfusion.attendance.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synfusion.attendance.R
import com.synfusion.attendance.ui.onboarding.components.OnboardingBottomControls
import com.synfusion.attendance.ui.onboarding.components.OnboardingProgress
import com.synfusion.attendance.ui.onboarding.steps.AttendanceStep
import com.synfusion.attendance.ui.onboarding.steps.EarningsStep
import com.synfusion.attendance.ui.onboarding.steps.FinalIntroStep
import com.synfusion.attendance.ui.onboarding.steps.PassbookStep
import com.synfusion.attendance.ui.onboarding.steps.WelcomeStep
import com.synfusion.attendance.ui.responsive.ProvideWindowSizeInfo
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.viewmodel.OfflineOnboardingViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private const val ONBOARDING_PAGE_COUNT = 5

@Composable
fun OnboardingScreen(
    viewModel: OfflineOnboardingViewModel,
    onOnboardingComplete: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = viewModel.state.value)
    val pagerState = rememberPagerState(
        initialPage = state.currentStep.coerceIn(0, ONBOARDING_PAGE_COUNT - 1),
        pageCount = { ONBOARDING_PAGE_COUNT }
    )
    val scope = rememberCoroutineScope()
    val pagerPage = pagerState.currentPage
    val controlPage = state.currentStep.coerceIn(0, ONBOARDING_PAGE_COUNT - 1)

    fun animateToPage(targetPage: Int) {
        val safeTarget = targetPage.coerceIn(0, ONBOARDING_PAGE_COUNT - 1)
        if (safeTarget == pagerState.currentPage || pagerState.isScrollInProgress) return
        scope.launch {
            pagerState.animateScrollToPage(safeTarget)
        }
    }

    BackHandler(enabled = pagerPage > 0) {
        animateToPage(pagerPage - 1)
    }

    LaunchedEffect(state.currentStep) {
        val target = state.currentStep.coerceIn(0, ONBOARDING_PAGE_COUNT - 1)
        if (!pagerState.isScrollInProgress && pagerState.currentPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(pagerState.isScrollInProgress, pagerState.currentPage) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != state.currentStep) {
            viewModel.goToStep(pagerState.currentPage)
        }
    }

    ProvideWindowSizeInfo {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                OnboardingProgress(
                    currentPage = controlPage,
                    pageCount = ONBOARDING_PAGE_COUNT
                )
            },
            contentWindowInsets = WindowInsets(0.dp)
        ) { innerPadding ->
            ResponsiveContainer {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { page ->
                        val rawOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                            .coerceIn(-1f, 1f)
                        val distance = rawOffset.absoluteValue

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .offset(x = (rawOffset * 18f).dp)
                                .graphicsLayer {
                                    val scale = 1f - (0.055f * distance)
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 1f - (0.30f * distance)
                                    rotationZ = rawOffset * 1.1f
                                    translationY = distance * 10f
                                }
                        ) {
                            when (page) {
                                0 -> WelcomeStep()
                                1 -> AttendanceStep()
                                2 -> EarningsStep()
                                3 -> PassbookStep()
                                4 -> FinalIntroStep()
                            }
                        }
                    }

                    OnboardingBottomControls(
                        showBack = controlPage > 0,
                        backText = stringResource(R.string.onboarding_back),
                        nextText = if (controlPage == ONBOARDING_PAGE_COUNT - 1) {
                            stringResource(R.string.onboarding_get_started)
                        } else {
                            stringResource(R.string.onboarding_next)
                        },
                        onBack = {
                            animateToPage(controlPage - 1)
                        },
                        onNext = {
                            if (controlPage == ONBOARDING_PAGE_COUNT - 1) {
                                viewModel.completeOnboarding {
                                    onOnboardingComplete()
                                }
                            } else {
                                animateToPage(controlPage + 1)
                            }
                        }
                    )
                }
            }
        }
    }
}
