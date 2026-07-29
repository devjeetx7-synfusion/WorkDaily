package com.synfusion.attendance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import java.util.Calendar
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.viewmodel.OfflineWorkerItem
import com.synfusion.attendance.viewmodel.OfflineWorkersViewModel
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkersScreen(
    viewModel: OfflineWorkersViewModel,
    onNavigateToWorkerDetail: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showFormDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var editingWorker by remember { mutableStateOf<OfflineWorkerItem?>(null) }

    val filteredWorkers = state.workers.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery) || it.workType.contains(searchQuery, ignoreCase = true)
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(pullRefreshState.isRefreshing) {
            viewModel.refresh()
        }
    }
    LaunchedEffect(state.isRefreshing) {
        if (state.isRefreshing) {
            pullRefreshState.startRefresh()
        } else {
            pullRefreshState.endRefresh()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        if (state.isLoading && state.role.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.manage_workers), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.search_by_name_phone_or_role)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (state.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (filteredWorkers.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (state.workers.isEmpty()) "No workers found" else "No matching workers", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (state.workers.isEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = {
                                        editingWorker = null
                                        showFormDialog = true
                                    }, shape = RoundedCornerShape(12.dp)) {
                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.add_worker))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredWorkers) { worker ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onNavigateToWorkerDetail(worker.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                            Text(worker.name.take(1).uppercase(Locale.getDefault()), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(worker.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text(worker.workType, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(onClick = { onNavigateToWorkerDetail(worker.id) }) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = "View Details")
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.daily_wage), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                        Text("₹${com.synfusion.attendance.ui.components.CurrencyAmountVisualTransformation.formatIndianCurrency(worker.wage)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                    }
                                    if (worker.phone.isNotEmpty()) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.phone), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                            Text(worker.phone, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { editingWorker = worker; showFormDialog = true },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.edit), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { showDeleteDialog = worker.id },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.delete), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.primary
        )

        FloatingActionButton(
            onClick = {
                editingWorker = null
                showFormDialog = true
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 64.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Worker")
        }

        if (showFormDialog) {
            OfflineWorkerFormDialog(
                worker = editingWorker,
                onDismiss = { showFormDialog = false; editingWorker = null },
                onSave = {
                    viewModel.saveWorker(it)
                    showFormDialog = false
                    editingWorker = null
                },
                isSaving = state.isSaving
            )
        }

        if (showDeleteDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.delete_worker), fontWeight = FontWeight.Bold) },
                text = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.are_you_sure_you_want_to_permanently_del_msg)) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.deleteWorker(showDeleteDialog!!); showDeleteDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.cancel)) }
                }
            )
        }
    }
}

class OfflineAadhaarTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 12) text.text.substring(0..11) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 4 == 3 && i != 11) out += " "
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 7) return offset + 1
                if (offset <= 11) return offset + 2
                return 14
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 9) return offset - 1
                if (offset <= 14) return offset - 2
                return 12
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineWorkerFormDialog(
    worker: OfflineWorkerItem?,
    onDismiss: () -> Unit,
    onSave: (OfflineWorkerItem) -> Unit,
    isSaving: Boolean
) {
    var name by remember { mutableStateOf(worker?.name ?: "") }
    var phone by remember { mutableStateOf(worker?.phone ?: "") }
    var aadhar by remember { mutableStateOf(worker?.aadhar ?: "") }
    var age by remember { mutableStateOf(worker?.age ?: "") }
    var workType by remember { mutableStateOf(worker?.workType ?: "Labour") }
    var wage by remember { mutableStateOf(worker?.wage?.toInt()?.toString() ?: "500") }

    val defaultWorkTypes = listOf("Labour", "Helper", "Mistry", "Custom")
    var expandedMenu by remember { mutableStateOf(false) }
    var isCustomType by remember { mutableStateOf(false) }
    var customTypeStr by remember { mutableStateOf("") }

    val isPhoneError = phone.isNotEmpty() && phone.length < 10
    val isAadhaarError = aadhar.isNotEmpty() && aadhar.length < 12
    val ageInt = age.toIntOrNull()
    val isAgeError = age.isNotEmpty() && (ageInt == null || ageInt < 14)

    val isFormValid = name.isNotBlank() && wage.isNotBlank() && !isPhoneError && !isAadhaarError && !isAgeError

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (worker == null) "Add New Worker" else "Edit Worker", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.full_name_1)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { newValue ->
                        val digits = newValue.filter { it.isDigit() }.take(10)
                        phone = digits
                    },
                    label = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.phone_number)) },
                    leadingIcon = { Text("+91 ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    isError = isPhoneError,
                    supportingText = if (isPhoneError) { { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.enter_valid_10digit_mobile_number)) } } else null
                )

                OutlinedTextField(
                    value = aadhar,
                    onValueChange = { newValue ->
                        val digits = newValue.filter { it.isDigit() }.take(12)
                        aadhar = digits
                    },
                    label = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.aadhaar_number_optional)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    visualTransformation = OfflineAadhaarTransformation(),
                    isError = isAadhaarError,
                    supportingText = if (isAadhaarError) { { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.aadhaar_must_be_exactly_12_digits)) } } else null
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = wage,
                        onValueChange = { wage = it.filter { ch -> ch.isDigit() } },
                        label = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.daily_wage_1)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = com.synfusion.attendance.ui.components.CurrencyAmountVisualTransformation(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Text("₹") }
                    )

                    OutlinedTextField(
                        value = age,
                        onValueChange = { newValue ->
                            val digits = newValue.filter { it.isDigit() }.take(2)
                            age = digits
                        },
                        label = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.age)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = isAgeError,
                        supportingText = if (isAgeError) { { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.must_be_14)) } } else null
                    )
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedMenu = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isCustomType) "Custom Type" else workType, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                        defaultWorkTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    expandedMenu = false
                                    if (type == "Custom") {
                                        isCustomType = true
                                    } else {
                                        workType = type
                                        isCustomType = false
                                    }
                                }
                            )
                        }
                    }
                }

                if (isCustomType) {
                    OutlinedTextField(
                        value = customTypeStr,
                        onValueChange = { customTypeStr = it },
                        label = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.custom_work_type)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalWorkType = if (isCustomType) customTypeStr else workType
                    val finalWage = wage.toDoubleOrNull() ?: 500.0
                    onSave(
                        OfflineWorkerItem(
                            id = worker?.id ?: "",
                            name = name.trim(),
                            phone = phone.trim(),
                            aadhar = aadhar.trim(),
                            age = age.trim(),
                            workType = finalWorkType.trim(),
                            wage = finalWage
                        )
                    )
                },
                enabled = !isSaving && isFormValid
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.cancel)) }
        }
    )
}
