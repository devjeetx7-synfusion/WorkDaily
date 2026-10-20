package com.synfusion.attendance.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.live.ui.UserListType
import com.synfusion.attendance.live.ui.components.FollowButton
import com.synfusion.attendance.live.ui.components.LivePrimaryEmerald
import com.synfusion.attendance.live.ui.components.WorkDailyLiveAvatar
import com.synfusion.attendance.live.viewmodel.LiveViewModel
import com.synfusion.attendance.rewards.ui.components.RewardCoinBalance
import com.synfusion.attendance.rewards.ui.components.RewardsPurple
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel
import com.synfusion.attendance.ui.components.CountrySelectionBottomSheet
import com.synfusion.attendance.ui.components.CropScreenDialog
import com.synfusion.attendance.ui.responsive.LocalWindowSizeInfo
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import com.synfusion.attendance.utils.CurrencyVisualTransformation
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.viewmodel.OfflineSettingsState
import com.synfusion.attendance.viewmodel.OfflineSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: OfflineSettingsViewModel,
    rewardsViewModel: RewardsViewModel? = null,
    liveViewModel: LiveViewModel? = null,
    onNavigateBack: () -> Unit,
    onNavigateWallet: () -> Unit = {},
    onNavigateRewardsHub: () -> Unit = {},
    onNavigateCreatorStudio: () -> Unit = {},
    onNavigateCreatorEarnings: () -> Unit = {},
    onNavigateGoLive: () -> Unit = {},
    onNavigateLeaderboard: () -> Unit = {},
    onNavigateMessages: () -> Unit = {},
    onNavigatePeople: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = OfflineSettingsState())
    val walletSummary by rewardsViewModel?.walletSummary?.collectAsState() ?: remember { mutableStateOf(null) }
    val creatorEarning by liveViewModel?.creatorEarning?.collectAsState() ?: remember { mutableStateOf(null) }
    val followedIds by liveViewModel?.followedCreatorIds?.collectAsState() ?: remember { mutableStateOf(emptySet()) }
    val publicUsers by liveViewModel?.publicUsers?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedUriForCrop by remember { mutableStateOf<Uri?>(null) }
    var showPhotoActionSheet by remember { mutableStateOf(false) }
    var showCountryBottomSheet by remember { mutableStateOf(false) }
    var activeUserListType by remember { mutableStateOf<UserListType?>(null) }
    var showBlockedUsersSheet by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUriForCrop = uri
        }
    }

    val hasChanges = state.name != state.originalName ||
            state.dailyWage != state.originalWage ||
            state.role != state.originalRole ||
            state.phone != state.originalPhone ||
            state.countryCode != state.originalCountryCode ||
            state.currencyCode != state.originalCurrencyCode ||
            state.profileImageUri != null ||
            state.profileImageUrl != state.originalProfileImageUrl

    val activeCountry = remember(state.countryCode) { CountryRepository.getCountry(state.countryCode) }
    val currencySymbol = remember(state.currencyCode) { CountryRepository.getCurrencySymbol(state.currencyCode) }

    val displayUri = state.profileImageUri
    val currentPhotoPath = state.profileImageUrl
    val googlePhotoUrl = state.googlePhotoUrl

    val imageModel: Any? = remember(displayUri, currentPhotoPath, googlePhotoUrl) {
        if (displayUri != null) displayUri
        else if (currentPhotoPath.isNotEmpty()) java.io.File(currentPhotoPath)
        else if (googlePhotoUrl.isNotEmpty()) googlePhotoUrl
        else null
    }

    val followerCount = remember(followedIds) { followedIds.size + 142 }
    val followingCount = 240
    val friendsCount = 35

    LaunchedEffect(state.savedMessage) {
        state.savedMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.profile_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges) {
                            viewModel.discardChanges()
                        }
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.onboarding_back)
                        )
                    }
                },
                actions = {
                    AnimatedVisibility(visible = hasChanges) {
                        IconButton(
                            onClick = { viewModel.saveChanges() },
                            enabled = !state.isSaving
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ResponsiveContainer {
                val windowSizeInfo = LocalWindowSizeInfo.current
                val isLandscape = windowSizeInfo.isLandscape

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // SECTION A — PROFILE HEADER
                    var animatePhoto by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { animatePhoto = true }
                    val animatedScale by animateFloatAsState(
                        targetValue = if (animatePhoto) 1f else 0.75f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "profilePhotoScale"
                    )

                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .graphicsLayer {
                                scaleX = animatedScale
                                scaleY = animatedScale
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape)
                                .clickable { showPhotoActionSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            val imageRequest = remember(imageModel) {
                                if (imageModel != null) {
                                    val builder = coil.request.ImageRequest.Builder(context)
                                        .data(imageModel)
                                        .crossfade(true)
                                    if (imageModel is java.io.File) {
                                        builder.memoryCacheKey("${imageModel.absolutePath}_${imageModel.lastModified()}")
                                            .diskCacheKey("${imageModel.absolutePath}_${imageModel.lastModified()}")
                                    }
                                    builder.networkCachePolicy(coil.request.CachePolicy.DISABLED)
                                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .build()
                                } else null
                            }

                            if (imageModel != null && imageRequest != null) {
                                coil.compose.SubcomposeAsyncImage(
                                    model = imageRequest,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    error = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Default Avatar",
                                            modifier = Modifier.size(56.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Default Avatar",
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .clickable { showPhotoActionSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Edit Profile Photo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = state.name.ifEmpty { androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.user_name) },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Member",
                                tint = LivePrimaryEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "@${state.name.lowercase().replace(" ", "") .ifEmpty { "user" }}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "WorkDaily Verified Member • Site Operations Specialist",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // SECTION B — SOCIAL COUNTERS
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { activeUserListType = UserListType.FOLLOWERS }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$followerCount",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Followers", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { activeUserListType = UserListType.FOLLOWING }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$followingCount",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Following", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { activeUserListType = UserListType.FRIENDS }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$friendsCount",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Friends", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // SECTION C — PERSONAL INFORMATION
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.personal_info),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = state.name,
                                onValueChange = { viewModel.onNameChange(it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.full_name)) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )

                            // Country Selector
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = "${activeCountry.flagEmoji} ${activeCountry.displayName} — ${activeCountry.currencySymbol}",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.country_region)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showCountryBottomSheet = true },
                                    shape = RoundedCornerShape(14.dp),
                                    leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { showCountryBottomSheet = true }
                                )
                            }

                            OutlinedTextField(
                                value = if (state.dailyWage > 0) state.dailyWage.toInt().toString() else "",
                                onValueChange = { viewModel.onWageChange(it.filter { ch -> ch.isDigit() }) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.daily_wage)) },
                                leadingIcon = { Text(currencySymbol, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = CurrencyVisualTransformation(state.currencyCode)
                            )

                            OutlinedTextField(
                                value = state.phone,
                                onValueChange = { newValue ->
                                    val digits = newValue.filter { it.isDigit() }.take(15)
                                    viewModel.onPhoneChange(digits)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.phone_number)) },
                                leadingIcon = { Text("${activeCountry.callingCode} ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    // SECTION D — REWARDS & WALLET
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RewardsPurple.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rewards & Wallet",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RewardsPurple
                                )
                                TextButton(onClick = onNavigateRewardsHub) {
                                    Text("Rewards Hub ›", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RewardsPurple)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Available Coins Card
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onNavigateWallet() },
                                    color = RewardsPurple.copy(alpha = 0.08f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, RewardsPurple.copy(alpha = 0.25f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Available Coins", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        RewardCoinBalance(
                                            amount = walletSummary?.totalBalance ?: 150,
                                            fontSize = 18,
                                            textColor = RewardsPurple,
                                            coinSize = 20.dp
                                        )
                                    }
                                }

                                // Live Diamonds Card
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onNavigateWallet() },
                                    color = LivePrimaryEmerald.copy(alpha = 0.08f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryEmerald.copy(alpha = 0.25f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Live Diamonds", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${creatorEarning?.availableDiamonds ?: 500} 💎",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = LivePrimaryEmerald
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION E — CREATOR EARNINGS
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryEmerald.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Creator Earnings",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LivePrimaryEmerald
                                )
                                TextButton(onClick = onNavigateCreatorStudio) {
                                    Text("Studio ›", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LivePrimaryEmerald)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Lifetime Earned", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "₹${(creatorEarning?.totalRupeesEarned ?: 600.0).toInt()}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = LivePrimaryEmerald
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Available Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${creatorEarning?.availableDiamonds ?: 900} 💎",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = onNavigateCreatorEarnings,
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryEmerald)
                            ) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = LivePrimaryEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cashout & Earnings Details", color = LivePrimaryEmerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    // SECTION F — SOCIAL & LIVE SHORTCUTS
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Social & Live Hub",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onNavigateGoLive,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Podcasts, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Go Live", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = onNavigateLeaderboard,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Leaderboard", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onNavigatePeople,
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("People & Friends", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }

                                OutlinedButton(
                                    onClick = onNavigateMessages,
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Messages", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    // SECTION G — ACCOUNT & PRIVACY
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Account & Privacy",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text("Google Account", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = if (state.googlePhotoUrl.isNotEmpty()) "Connected" else "Local Workspace Session",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LivePrimaryEmerald, modifier = Modifier.size(20.dp))
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showBlockedUsersSheet = true }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Blocked Users", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Button(
                                onClick = onLogout,
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Log Out", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    // Save Action Button
                    if (hasChanges) {
                        Button(
                            onClick = { viewModel.saveChanges() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !state.isSaving
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.save_changes),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Crop Screen Dialog when an image is selected
            if (selectedUriForCrop != null) {
                CropScreenDialog(
                    imageUri = selectedUriForCrop!!,
                    onCropSuccess = { croppedUri ->
                        viewModel.onProfileImageSelected(croppedUri)
                        selectedUriForCrop = null
                    },
                    onCancel = {
                        selectedUriForCrop = null
                    }
                )
            }

            // Photo Action Sheet Modal
            if (showPhotoActionSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showPhotoActionSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    windowInsets = WindowInsets(0.dp),
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.profile_photo),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showPhotoActionSheet = false
                                    photoPickerLauncher.launch("image/*")
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.change_photo),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (imageModel != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showPhotoActionSheet = false
                                        viewModel.onProfileImageSelected(null)
                                        viewModel.onRemoveProfilePhoto()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.remove_photo),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            if (showCountryBottomSheet) {
                CountrySelectionBottomSheet(
                    selectedCountryCode = state.countryCode,
                    onCountrySelected = { country ->
                        viewModel.onCountryChange(country.countryCode)
                        showCountryBottomSheet = false
                    },
                    onDismiss = { showCountryBottomSheet = false }
                )
            }

            if (activeUserListType != null) {
                val listType = activeUserListType!!
                ModalBottomSheet(
                    onDismissRequest = { activeUserListType = null },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    val title = when (listType) {
                        UserListType.FOLLOWERS -> "Followers"
                        UserListType.FOLLOWING -> "Following"
                        UserListType.FRIENDS -> "Friends"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            IconButton(onClick = { activeUserListType = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        val listUsers = remember(publicUsers, listType) {
                            when (listType) {
                                UserListType.FRIENDS -> publicUsers.filter { it.isFriend }
                                UserListType.FOLLOWING -> publicUsers.filter { it.isFollowing }
                                UserListType.FOLLOWERS -> publicUsers
                            }
                        }

                        if (listUsers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No users found in $title", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 360.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(listUsers, key = { it.id }) { user ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .clickable {
                                                activeUserListType = null
                                                onNavigateMessages()
                                            }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            WorkDailyLiveAvatar(
                                                name = user.name,
                                                avatarUrl = user.avatar,
                                                size = 40.dp,
                                                isLive = user.isLiveNow,
                                                showBadge = user.isLiveNow
                                            )
                                            Column {
                                                Text(user.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                Text(user.profession, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        FollowButton(
                                            isFollowing = user.isFollowing,
                                            onToggle = { liveViewModel?.toggleFollowCreator(user.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showBlockedUsersSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBlockedUsersSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Blocked Users", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("No blocked users in your account.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
