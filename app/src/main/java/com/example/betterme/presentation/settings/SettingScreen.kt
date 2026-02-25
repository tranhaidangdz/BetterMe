package com.example.betterme.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    navigateBack: () -> Unit,
    logout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Settings Screen")

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = navigateBack) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = logout) {
            Text("Logout → Go to SignIn")
        }
    }
}
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        navigateBack = {},
        logout = {}
    )
}