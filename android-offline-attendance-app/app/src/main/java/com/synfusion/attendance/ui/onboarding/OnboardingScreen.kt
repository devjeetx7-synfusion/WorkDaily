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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synfusion.attendance.R
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
    var showCountryBottomSheet by remember { mutableStateOf(false) }

    // Intercept hardware back button when on steps 1..4
    BackHandler(enabled = state.currentStep in 1..4) {
        viewModel.onPreviousStep()
    }

    ProvideWindowSizeInfo {
        Scaffold(
            topBar = {
                if (state.currentStep in 0..4) {
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
                                        .size(if (index == state.currentStep) 10.dp else 8.dp)
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
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 6 })

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
                        userScrollEnabled = state.currentStep < 5 // disable swipe on the reveal step
                    ) { step ->
                        when (step) {
                            0 -> WelcomeStep(
                                onNext = { viewModel.onNextStep() }
                            )

                            1 -> ProfileStep(
                                name = state.name,
                                profileImageUri = state.profileImageUri,
                                onNameChange = { viewModel.updateName(it) },
                                onPhotoSelected = { viewModel.updateProfilePhoto(it) },
                                onNext = { viewModel.onNextStep() }
                            )

                            2 -> CountryStep(
                                selectedCountry = state.selectedCountry,
                                onChangeCountryClick = { showCountryBottomSheet = true },
                                onNext = { viewModel.onNextStep() }
                            )

                            3 -> WageStep(
                                dailyWage = state.dailyWage,
                                selectedCountry = state.selectedCountry,
                                onWageChange = { viewModel.updateWage(it) },
                                onNext = { viewModel.onNextStep() }
                            )

                            4 -> MobileStep(
                                phoneNumber = state.phoneNumber,
                                selectedCountry = state.selectedCountry,
                                onPhoneChange = { viewModel.updatePhone(it) },
                                onNext = { viewModel.onNextStep() }
                            )

                            5 -> ProfileRevealStep(
                                name = state.name,
                                profileImageUri = state.profileImageUri,
                                selectedCountry = state.selectedCountry,
                                dailyWage = state.dailyWage,
                                onComplete = {
                                    viewModel.completeOnboarding {
                                        onOnboardingComplete()
                                    }
                                }
                            )
                        }
                    }
                }

                if (showCountryBottomSheet) {
                    CountrySelectionBottomSheet(
                        selectedCountry = state.selectedCountry,
                        onCountrySelected = { country ->
                            viewModel.updateCountry(country)
                        },
                        onDismiss = { showCountryBottomSheet = false }
                    )
                }
            }
        }
    }
}
