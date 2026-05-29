package com.dailywork.attedance.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywork.attedance.ui.theme.DailyWorkTheme
import kotlin.system.exitProcess

class CrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val errorTrace = intent.getStringExtra(EXTRA_ERROR_INFO) ?: "Unknown Error"
        val crashTime = intent.getStringExtra(EXTRA_CRASH_TIME) ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        val appVersion = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "Unknown" }
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        setContent {
            DailyWorkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CrashScreen(
                        errorTrace = errorTrace,
                        appVersion = appVersion,
                        deviceInfo = "$deviceModel, $osVersion",
                        crashTime = crashTime,
                        onRestart = {
                            val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                            if (intent != null) {
                                startActivity(intent)
                            }
                            finish()
                            exitProcess(0)
                        },
                        onClose = {
                            finishAffinity()
                            exitProcess(0)
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_ERROR_INFO = "EXTRA_ERROR_INFO"
        const val EXTRA_CRASH_TIME = "EXTRA_CRASH_TIME"
    }
}

@Composable
fun CrashScreen(errorTrace: String, appVersion: String, deviceInfo: String, crashTime: String, onRestart: () -> Unit, onClose: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Warning),
            contentDescription = "App Logo",
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Oops! Something went wrong",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "An unexpected error occurred. We apologize for the inconvenience.",
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(text = "App Version: $appVersion", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "Device Info: $deviceInfo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "Crash Time: $crashTime", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            SelectionContainer {
                Text(
                    text = errorTrace,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(errorTrace))
                Toast.makeText(context, "Error trace copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.copy_error))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f)
            ) {
                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.close_app))
            }
            Button(
                onClick = onRestart,
                modifier = Modifier.weight(1f)
            ) {
                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.restart))
            }
        }
    }
}
