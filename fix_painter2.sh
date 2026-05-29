#!/bin/bash
sed -i 's/painter = androidx.compose.ui.res.painterResource(id = com.dailywork.attedance.R.mipmap.ic_launcher),/painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.AccountCircle),/g' ./android-app/app/src/main/java/com/dailywork/attedance/ui/SettingsScreen.kt
sed -i 's/painter = painterResource(id = com.dailywork.attedance.R.mipmap.ic_launcher),/painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Warning),/g' ./android-app/app/src/main/java/com/dailywork/attedance/ui/CrashActivity.kt
