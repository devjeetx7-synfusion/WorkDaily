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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.live.ui.UserListType
import com.synfusion.attendance.live.ui.UserRelationshipBottomSheet
import com.synfusion.attendance.live.ui.components.FollowButton
import com.synfusion.attendance.live.ui.components.LivePrimaryEmerald
import com.synfusion.attendance.live.ui.components.WorkDailyLiveAvatar
import com.synfusion.attendance.live.viewmodel.LiveViewModel
import com.synfusion.attendance.online.ProfileOperationResult
import com.synfusion.attendance.online.SocialSessionCache
import com.synfusion.attendance.online.rememberCurrentFirebaseUid
import com.synfusion.attendance.rewards.ui.components.RewardCoinBalance
import com.synfusion.attendance.rewards.ui.components.RewardsPurple
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel
import com.synfusion.attendance.ui.components.CropScreenDialog
import com.synfusion.attendance.ui.components.GenderSymbol
import com.synfusion.attendance.ui.components.ProfileCountValueSkeleton
import com.synfusion.attendance.ui.components.WorkDailySubtleRefreshBar
import com.synfusion.attendance.ui.responsive.LocalWindowSizeInfo
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import com.synfusion.attendance.utils.AmountFormatter
import com.synfusion.attendance.utils.DateOfBirthUtils
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.viewmodel.OfflineSettingsState
import com.synfusion.attendance.viewmodel.OfflineSettingsViewModel
import kotlinx.coroutines.launch

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
    onNavigateUserProfile: (String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = OfflineSettingsState())
    val walletSummary by rewardsViewModel?.walletSummary?.collectAsState() ?: remember { mutableStateOf(null) }
    val creatorEarning by liveViewModel?.creatorEarning?.collectAsState() ?: remember { mutableStateOf(null) }
    // The viewer's own diamond wallet. Distinct from creatorEarning.availableDiamonds, which is
    // what a creator has earned and can cash out -- two different balances that must not be
    // substituted for one another.
    val diamondWallet by liveViewModel?.wallet?.collectAsState() ?: remember { mutableStateOf(null) }
    val followedIds by liveViewModel?.followedCreatorIds?.collectAsState() ?: remember { mutableStateOf(emptySet()) }
    val publicUsers by liveViewModel?.publicUsers?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedUriForCrop by remember { mutableStateOf<Uri?>(null) }
    // The photo action sheet is gone (see the preview dialog below); the picker and crop chain it
    // used to trigger are deliberately retained so the upload implementation still exists.
    var showPhotoPreview by remember { mutableStateOf(false) }
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

    // Hoisted from inside the avatar Box so the preview dialog renders through the very same Coil
    // request -- same cache keys, same crossfade, same file-vs-URL cache policy. Building a second
    // request for the dialog would have duplicated that tuning and let the two drift apart.
    val imageRequest = remember(imageModel) {
        if (imageModel != null) {
            val builder = coil.request.ImageRequest.Builder(context)
                .data(imageModel)
                .crossfade(true)
            if (imageModel is java.io.File) {
                val key = "${imageModel.absolutePath}_${imageModel.lastModified()}"
                builder.memoryCacheKey(key)
                    .diskCacheKey(key)
                    .networkCachePolicy(coil.request.CachePolicy.DISABLED)
            } else {
                val key = imageModel.toString()
                builder.memoryCacheKey(key)
                    .diskCacheKey(key)
                    .networkCachePolicy(coil.request.CachePolicy.ENABLED)
            }
            builder.diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .build()
        } else null
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val currentUserId = rememberCurrentFirebaseUid().value
    val socialSnapshot by SocialSessionCache.manager.snapshot.collectAsStateWithLifecycle(
        initialValue = SocialSessionCache.manager.snapshot.value
    )

    val followRepo = SocialSessionCache.followRepository
    val blockRepo = SocialSessionCache.blockRepository

    val cachedCountsEntry = remember(socialSnapshot, currentUserId) {
        val uid = currentUserId
        if (!uid.isNullOrBlank() && socialSnapshot.ownerUid == uid) {
            socialSnapshot.followCounts[uid]
        } else {
            null
        }
    }
    val cachedOwnProfile = remember(socialSnapshot, currentUserId) {
        val uid = currentUserId
        if (!uid.isNullOrBlank() && socialSnapshot.ownerUid == uid) {
            socialSnapshot.profiles[uid]?.value
        } else {
            null
        }
    }

    var isCountsLoading by remember(currentUserId) {
        mutableStateOf(!currentUserId.isNullOrBlank() && cachedCountsEntry == null)
    }
    var countsLoadError by remember(currentUserId) { mutableStateOf<String?>(null) }

    suspend fun refreshOwnFollowCounts(uid: String?, forceRefresh: Boolean) {
        if (uid.isNullOrBlank()) {
            isCountsLoading = false
            countsLoadError = null
            return
        }
        isCountsLoading = true
        countsLoadError = null
        try {
            when (val countsRes = SocialSessionCache.manager.getFollowCounts(uid, uid, forceRefresh = forceRefresh)) {
                is ProfileOperationResult.Success -> {
                    countsLoadError = null
                }
                is ProfileOperationResult.Error -> {
                    val hasCached = SocialSessionCache.manager.currentSnapshotFor(uid)?.followCounts?.get(uid) != null
                    if (!hasCached) {
                        countsLoadError = countsRes.message
                    }
                }
            }
        } finally {
            isCountsLoading = false
        }
    }

    LaunchedEffect(currentUserId) {
        refreshOwnFollowCounts(currentUserId, forceRefresh = false)
    }

    DisposableEffect(lifecycleOwner, currentUserId) {
        var handledInitialResume = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (!handledInitialResume) {
                    handledInitialResume = true
                } else if (!currentUserId.isNullOrBlank()) {
                    coroutineScope.launch {
                        refreshOwnFollowCounts(currentUserId, forceRefresh = false)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val followerCount = cachedCountsEntry?.value?.followerCount ?: 0
    val followingCount = cachedCountsEntry?.value?.followingCount ?: 0
    val showCountsSkeleton = !currentUserId.isNullOrBlank() && cachedCountsEntry == null && isCountsLoading
    val showCountsError = !currentUserId.isNullOrBlank() && cachedCountsEntry == null && !isCountsLoading && countsLoadError != null

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
        containerColor = MaterialTheme.colorScheme.background,
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
                                .clickable(enabled = imageModel != null) { showPhotoPreview = true },
                            contentAlignment = Alignment.Center
                        ) {
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

                        // The camera badge that opened the change-photo sheet is removed along with
                        // it, so no affordance on this screen still offers to change the picture.
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
                            // Renders nothing when gender is unset or unrecognised, so the profile
                            // never implies a gender the user did not choose.
                            GenderSymbol(gender = state.gender, size = 18.dp)
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Member",
                                tint = LivePrimaryEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        val resolvedHandle = state.username
                            .ifEmpty { cachedOwnProfile?.username.orEmpty() }
                            .ifEmpty { state.name.lowercase().replace(" ", "").ifEmpty { "user" } }

                        Text(
                            text = "@$resolvedHandle",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = LivePrimaryEmerald
                        )

                        // Human-readable age instead of the raw date of birth. Derived on every
                        // recomposition from the stored date, so it advances on the user's birthday
                        // with nothing cached to go stale, and age is never persisted in place of
                        // the date itself.
                        DateOfBirthUtils.ageLabel(state.dateOfBirth)?.let { ageLabel ->
                            Text(
                                text = ageLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

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
                        Column(modifier = Modifier.fillMaxWidth()) {
                            WorkDailySubtleRefreshBar(
                                visible = isCountsLoading && cachedCountsEntry != null
                            )

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
                                        .clickable {
                                            if (!currentUserId.isNullOrBlank()) {
                                                activeUserListType = UserListType.FOLLOWERS
                                            } else {
                                                onNavigatePeople()
                                            }
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    when {
                                        showCountsSkeleton -> {
                                            ProfileCountValueSkeleton()
                                            Spacer(modifier = Modifier.height(3.dp))
                                        }
                                        showCountsError -> {
                                            Text(
                                                text = "—",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = "$followerCount",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Text("Followers", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (!currentUserId.isNullOrBlank()) {
                                                activeUserListType = UserListType.FOLLOWING
                                            } else {
                                                onNavigatePeople()
                                            }
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    when {
                                        showCountsSkeleton -> {
                                            ProfileCountValueSkeleton()
                                            Spacer(modifier = Modifier.height(3.dp))
                                        }
                                        showCountsError -> {
                                            Text(
                                                text = "—",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = "$followingCount",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Text("Following", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            if (showCountsError) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            coroutineScope.launch {
                                                refreshOwnFollowCounts(currentUserId, forceRefresh = true)
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Couldn't load counts • Tap to retry",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
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
                                        // No invented fallback: a null walletSummary means no
                                        // rewards source was supplied, and rendering 150 there
                                        // showed a balance the user does not have.
                                        val availableCoins = walletSummary?.totalBalance
                                        if (availableCoins == null) {
                                            Text(
                                                text = "—",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            RewardCoinBalance(
                                                amount = availableCoins,
                                                fontSize = 18,
                                                textColor = RewardsPurple,
                                                coinSize = 20.dp
                                            )
                                        }
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
                                        // Sourced from the viewer's diamond wallet, not creator
                                        // earnings. The previous wiring displayed
                                        // creatorEarning.availableDiamonds here, so a creator's
                                        // cashable balance was presented as the user's spendable
                                        // diamonds.
                                        Text(
                                            text = diamondWallet?.diamondBalance
                                                ?.let { "$it 💎" }
                                                ?: "—",
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
                                            text = creatorEarning?.totalRupeesEarned
                                                ?.let { "${activeCountry.currencySymbol}${it.toInt()}" }
                                                ?: "—",
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
                                            text = creatorEarning?.availableDiamonds
                                                ?.let { "$it 💎" }
                                                ?: "—",
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

            // Profile photo preview. Tapping the avatar opens this instead of a change-photo sheet:
            // the image is shown at a fixed 1:1 frame with ContentScale.Fit, so the whole photo is
            // visible and never stretched or cropped, and it dismisses on the button, on scrim tap
            // and on back. The saved image, its URL and the avatar fallback are all untouched.
            if (showPhotoPreview) {
                AlertDialog(
                    onDismissRequest = { showPhotoPreview = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = {
                        Text(
                            text = androidx.compose.ui.res.stringResource(
                                com.synfusion.attendance.R.string.profile_photo
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageModel != null && imageRequest != null) {
                                coil.compose.SubcomposeAsyncImage(
                                    model = imageRequest,
                                    contentDescription = "Profile photo preview",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                    error = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Default Avatar",
                                            modifier = Modifier.size(64.dp),
                                            tint = LivePrimaryEmerald
                                        )
                                    }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Default Avatar",
                                    modifier = Modifier.size(64.dp),
                                    tint = LivePrimaryEmerald
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPhotoPreview = false }) {
                            Text("Close", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            val selectedOwnListType = activeUserListType
            val activeSheetUid = currentUserId?.takeIf { it.isNotBlank() }
            if (selectedOwnListType != null && activeSheetUid != null) {
                UserRelationshipBottomSheet(
                    targetUid = activeSheetUid,
                    listType = selectedOwnListType,
                    initialCount = if (selectedOwnListType == UserListType.FOLLOWERS) followerCount else followingCount,
                    currentUserId = activeSheetUid,
                    followRepo = followRepo,
                    blockRepo = blockRepo,
                    onDismiss = {
                        activeUserListType = null
                        coroutineScope.launch {
                            refreshOwnFollowCounts(currentUserId, forceRefresh = false)
                        }
                    },
                    onRelationshipChanged = {
                        coroutineScope.launch {
                            refreshOwnFollowCounts(currentUserId, forceRefresh = true)
                        }
                    },
                    onUserSelected = { selectedProfile ->
                        activeUserListType = null
                        if (selectedProfile.uid != currentUserId) {
                            onNavigateUserProfile(selectedProfile.uid)
                        }
                    }
                )
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
