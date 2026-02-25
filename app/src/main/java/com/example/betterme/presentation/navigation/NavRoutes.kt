package com.example.betterme.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
@Composable
fun NavRoutes()
{
    val backStack = rememberNavBackStack(Destination.Splash)

}