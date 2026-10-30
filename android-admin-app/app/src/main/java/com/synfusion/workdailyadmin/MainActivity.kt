package com.synfusion.workdailyadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synfusion.workdailyadmin.auth.AdminAuthState
import com.synfusion.workdailyadmin.auth.AdminAuthViewModel
import com.synfusion.workdailyadmin.auth.AdminLoginScreen
import com.synfusion.workdailyadmin.ui.navigation.AdminMainLayout
import com.synfusion.workdailyadmin.ui.theme.WorkDailyAdminTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WorkDailyAdminTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AdminAuthViewModel = viewModel()
                    val authState by authViewModel.authState.collectAsState()

                    if (authState is AdminAuthState.Authenticated) {
                        AdminMainLayout(authViewModel = authViewModel)
                    } else {
                        AdminLoginScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = { }
                        )
                    }
                }
            }
        }
    }
}
