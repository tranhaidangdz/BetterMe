package com.example.betterme.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.betterme.presentation.main.MainScreen
import com.example.betterme.presentation.onboarding.OnboardingScreen
import com.example.betterme.presentation.onboarding.WelcomeScreen
import com.example.betterme.presentation.settings.SettingsScreen
import com.example.betterme.presentation.signin.SignInScreen
import com.example.betterme.presentation.splash.SplashScreen
import com.example.betterme.utils.ext.replaceTop

@Composable
fun NavRoutes()
{
    val backStack = rememberNavBackStack(Destination.Splash)

    NavDisplay(
        backStack = backStack,
        onBack = {
            backStack.removeLastOrNull()
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Destination.Splash>{
                SplashScreen(
                    navigateToWelcome = {
                        backStack.replaceTop(Destination.Welcome)
                    },
                    navigateToMain = {
                        backStack.replaceTop(Destination.Main)
                    }
                )
            }

            entry<Destination.Welcome> {
                WelcomeScreen(
                    navigateToOnboarding = {
                        backStack.replaceTop(Destination.Onboarding)
                    }
                )
            }

            entry<Destination.Onboarding> {
                OnboardingScreen(
                    navigateToSignIn = {
                        backStack.replaceTop(Destination.SignIn)
                    }
                )
            }

            entry<Destination.SignIn> {
                SignInScreen(
                    navigateToMain = {
                        backStack.replaceTop(Destination.Main)
                    }
                )
            }

            entry<Destination.Main> {
                MainScreen(
                    navigateToSettings = {
                        backStack.add(Destination.Settings)
                    }
                )
            }

            entry<Destination.Settings> {
                SettingsScreen(
                    navigateBack = {
                        backStack.removeLastOrNull()
                    },
                    logout = {
                        backStack.add(Destination.SignIn)
                    }
                )
            }
        }
    )
}