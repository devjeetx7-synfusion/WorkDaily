package com.dailywork.attedance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywork.attedance.data.UserPreferencesRepository
import kotlinx.coroutines.launch

data class LanguageOption(val code: String, val name: String, val char: String, val color: Color)

@Composable
fun LanguageSelectionScreen(
    repository: UserPreferencesRepository,
    onLanguageSelected: () -> Unit
) {
    var selectedLanguageCode by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val languages = listOf(
        LanguageOption("hi", "Hindi", "अ", Color(0xFFFFE4E1)),
        LanguageOption("en", "English", "A", Color(0xFFE0F7FA))
    )
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = androidx.compose.ui.res.stringResource(id = com.dailywork.attedance.R.string.select_language),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = androidx.compose.ui.res.stringResource(id = com.dailywork.attedance.R.string.choose_your_preferred_language_to_continue),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(languages) { lang ->
                val isSelected = selectedLanguageCode == lang.code

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedLanguageCode = lang.code }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(lang.color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(lang.char, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha=0.7f))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(lang.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                selectedLanguageCode?.let {
                    scope.launch {
                        repository.saveLanguage(it)
                        com.dailywork.attedance.utils.LocaleHelper.setLocale(context, it)
                        onLanguageSelected()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = selectedLanguageCode != null
        ) {
            Text(androidx.compose.ui.res.stringResource(id = com.dailywork.attedance.R.string.continue_btn), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
