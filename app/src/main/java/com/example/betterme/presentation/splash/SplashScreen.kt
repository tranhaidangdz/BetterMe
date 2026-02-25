package com.example.betterme.presentation.splash

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SplashScreen(
    navigateToWelcome: () -> Unit,
    navigateToMain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text = "Splash Screen")

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = navigateToWelcome) {
            Text("Go to Welcome")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = navigateToMain) {
            Text("Skip to Main")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SpashScreenPreview()
{
    SplashScreen(
        navigateToWelcome = {},
        navigateToMain = {}
    )
}