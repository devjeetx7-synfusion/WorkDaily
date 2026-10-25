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
import com.synfusion.attendance.ui.components.CropScreenDialog
import com.synfusion.attendance.ui.responsive.LocalWindowSizeInfo
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import com.synfusion.attendance.utils.AmountFormatter
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
    onNavigateSettings: () -> Unit = {},
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
    var activeUserListType by remember { mutableStateOf<UserListType?>(null) }
    var showBlockedUsersSheet by remember { mutableStateOf(false) }
    var isPhoneMasked by remember { mutableStateOf(true) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUriForCrop = uri
        }
    }

    val activeCountry = remember(state.countryCode) { CountryRepository.getCountry(state.countryCode) }

    val displayUri = state.profileImageUri
    val currentPhotoPath = state.profileImageUrl
    val googlePhotoUrl = state.googlePhotoUrl

    val imageModel: Any? = remember(displayUri, currentPhotoPath, googlePhotoUrl) {
        if (displayUri != null) displayUri
        else if (currentPhotoPath.isNotEmpty()) java.io.File(currentPhotoPath)
        else if (googlePhotoUrl.isNotEmpty()) googlePhotoUrl
        else null
    }

    val currentUserId = remember {
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        } catch (e: Exception) {
            null
        }
    }

    var realFollowersCount by remember { mutableStateOf(0) }
    var realFollowingCount by remember { mutableStateOf(0) }
    val followRepo = remember { com.synfusion.attendance.online.FirestoreFollowRepository() }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            val followersRes = followRepo.getFollowers(currentUserId)
            if (followersRes is com.synfusion.attendance.online.ProfileOperationResult.Success) {
                realFollowersCount = followersRes.data.size
            }
            val followingRes = followRepo.getFollowing(currentUserId)
            if (followingRes is com.synfusion.attendance.online.ProfileOperationResult.Success) {
                realFollowingCount = followingRes.data.size
            }
        }
    }

    val followerCount = realFollowersCount
    val followingCount = realFollowingCount

    val maskedPhone = remember(state.phone, activeCountry) {
        if (state.phone.length > 4) {
            "${activeCountry.callingCode} ${"•".repeat(state.phone.length - 4)}${state.phone.takeLast(4)}"
        } else if (state.phone.isNotEmpty()) {
            "${activeCountry.callingCode} ••••"
        } else {
            "Not provided"
        }
    }

    val fullPhone = remember(state.phone, activeCountry) {
        if (state.phone.isNotEmpty()) "${activeCountry.callingCode} ${state.phone}" else "Not provided"
    }

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
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = com.synfusion.attendance.ui.components.icons.WorkDailyIcons.ArrowLeft,
                            contentDescription = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.onboarding_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateSettings) {
                        Icon(
                            imageVector = com.synfusion.attendance.ui.components.icons.WorkDailyIcons.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. TOOLBAR & HEADER IDENTITY
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
                            .padding(top = 4.dp)
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
                                .border(3.dp, LivePrimaryEmerald.copy(alpha = 0.35f), CircleShape)
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
                                            tint = LivePrimaryEmerald
                                        )
                                    }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Default Avatar",
                                    modifier = Modifier.size(56.dp),
                                    tint = LivePrimaryEmerald
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(LivePrimaryEmerald)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .clickable { showPhotoActionSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Edit Profile Photo",
                                tint = Color.White,
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
                            color = LivePrimaryEmerald
                        )

                        Text(
                            text = if (state.bio.isNotBlank()) state.bio else "WorkDaily Verified Member • ${state.profession.ifEmpty { "Site Worker" }}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    // 2. SOCIAL STATISTICS (Followers and Following)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
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
                                    .clickable { onNavigatePeople() }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$followerCount",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Followers", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onNavigatePeople() }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$followingCount",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Following", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // 3. READ-ONLY PERSONAL INFORMATION
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.personal_info),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Read-only Row 1: Country & Region
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Country & Region", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${activeCountry.flagEmoji} ${activeCountry.displayName} (${activeCountry.currencySymbol})",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            // Read-only Row 2: Daily Wage
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(LivePrimaryEmerald.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = activeCountry.currencySymbol,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LivePrimaryEmerald
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Daily Wage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (state.dailyWage > 0) AmountFormatter.formatWithSymbol(state.dailyWage, state.currencyCode) + " / day" else "Not configured",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            // Read-only Row 3: Masked Contact Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Private Phone Number", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (isPhoneMasked) maskedPhone else fullPhone,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = { isPhoneMasked = !isPhoneMasked }) {
                                    Icon(
                                        imageVector = if (isPhoneMasked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle phone masking",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 4. REWARDS & WALLET
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

                    // 5. CREATOR EARNINGS
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



                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Crop Screen Dialog when an image is selected
            if (selectedUriForCrop != null) {
                CropScreenDialog(
                    imageUri = selectedUriForCrop!!,
                    onCropSuccess = { croppedUri ->
                        viewModel.onProfileImageSelected(croppedUri)
                        viewModel.saveChanges()
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
                                        viewModel.saveChanges()
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
                        UserListType.FRIENDS -> "Followers"
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
                                UserListType.FOLLOWING -> publicUsers.filter { it.isFollowing }
                                else -> publicUsers
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
                                                showBadge = false
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
