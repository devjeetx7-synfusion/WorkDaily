package com.dailywork.attedance.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.dailywork.attedance.data.UserPreferencesRepository
import com.dailywork.attedance.ui.theme.DailyWorkTheme
import com.dailywork.attedance.utils.LocaleHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LanguageSelectorActivity : ComponentActivity() {

    private lateinit var repository: UserPreferencesRepository

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = UserPreferencesRepository(applicationContext)

        setContent {
            val userThemePreference by repository.darkModeFlow.collectAsState(initial = null)
            val isDarkMode = userThemePreference ?: isSystemInDarkTheme()
            DailyWorkTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var selectedLanguage by remember { mutableStateOf("en") }
                    var isLoading by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        val currentLang = repository.languageFlow.first() ?: "en"
                        selectedLanguage = currentLang
                        isLoading = false
                    }

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.language_1)) },
                                navigationIcon = {
                                    IconButton(onClick = { finish() }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                    }
                                }
                            )
                        }
                    ) { padding ->
                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.select_your_preferred_language),
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                val languages = listOf(
                                    "en" to "English",
                                    "hi" to "हिंदी"
                                )

                                languages.forEach { (code, name) ->
                                    val isSelected = selectedLanguage == code
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
                                            .border(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedLanguage = code }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = name,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                        )
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Button(
                                    onClick = {
                                        saveLanguageAndRestart(selectedLanguage)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.apply), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveLanguageAndRestart(langCode: String) {
        lifecycleScope.launch {
            repository.saveLanguage(langCode)
            LocaleHelper.setLocale(this@LanguageSelectorActivity, langCode)

            // Try saving to Firebase as well, but do not wait for it to restart
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    FirebaseFirestore.getInstance().collection("users").document(user.uid)
                        .set(mapOf("language" to langCode), SetOptions.merge())
                }
            } catch (e: Exception) {
                // Ignore failure
            }

            // Restart app using proper flags
            val intent = Intent(this@LanguageSelectorActivity, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            finish()
        }
    }
}
