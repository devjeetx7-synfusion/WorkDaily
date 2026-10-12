package com.synfusion.attendance.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.ui.components.CountrySelectionBottomSheet
import com.synfusion.attendance.ui.components.CropScreenDialog
import com.synfusion.attendance.ui.responsive.LocalWindowSizeInfo
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import com.synfusion.attendance.utils.AmountFormatter
import com.synfusion.attendance.utils.CurrencyVisualTransformation
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.viewmodel.OfflineSettingsState
import com.synfusion.attendance.viewmodel.OfflineSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: OfflineSettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = OfflineSettingsState())
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedUriForCrop by remember { mutableStateOf<Uri?>(null) }
    var showPhotoActionSheet by remember { mutableStateOf(false) }
    var showCountryBottomSheet by remember { mutableStateOf(false) }

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
                        text = "Profile",
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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

                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left Section: Photo and name
                        Column(
                            modifier = Modifier.weight(0.8f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(top = 8.dp),
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
                                                    .networkCachePolicy(coil.request.CachePolicy.DISABLED)
                                            } else {
                                                builder.networkCachePolicy(coil.request.CachePolicy.ENABLED)
                                            }
                                            builder.diskCachePolicy(coil.request.CachePolicy.ENABLED)
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
                                                    modifier = Modifier.size(50.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Default Avatar",
                                            modifier = Modifier.size(50.dp),
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
                                Text(
                                    text = state.name.ifEmpty { androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.user_name) },
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (state.phone.isNotEmpty()) {
                                    Text(
                                        text = "${activeCountry.callingCode} ${state.phone}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "100% Offline & Private",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Your details are saved locally.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Right Section: Editable Personal Info Form
                        Column(
                            modifier = Modifier.weight(1.2f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Personal Info",
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
                                            label = { Text("Country / Region") },
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

                            if (hasChanges) {
                                Button(
                                    onClick = { viewModel.saveChanges() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
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
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Profile Photo Header Section
                        var animatePhoto by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            animatePhoto = true
                        }
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
                                    .size(120.dp)
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
                                                modifier = Modifier.size(60.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Default Avatar",
                                        modifier = Modifier.size(60.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Circular edit/pencil button overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(36.dp)
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
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = state.name.ifEmpty { androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.user_name) },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (state.phone.isNotEmpty()) {
                                Text(
                                    text = "${activeCountry.callingCode} ${state.phone}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Structured Information Cards (WhatsApp-inspired clean fields)
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
                                    text = "Personal Info",
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
                                        label = { Text("Country / Region") },
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

                        // Storage & Privacy Badge Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "100% Offline & Private",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Your profile details are safely saved locally on your device.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
        }
    }
}
