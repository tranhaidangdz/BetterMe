package com.example.betterme.presentation.signin

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.betterme.R
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography
import org.koin.androidx.compose.koinViewModel

@Composable
fun SignInScreen(
    navigateToMain: () -> Unit,
    navigateToHabitSelection: () -> Unit,
    viewModel: SignInViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity

    // Collect single events
    LaunchedEffect(Unit) {
        viewModel.singleEvent.collect { event ->
            when (event) {
                SignInEvent.NavigateToHome -> navigateToMain()
                SignInEvent.NavigateToHabitSelection -> navigateToHabitSelection()
                SignInEvent.LoginError -> {
                    Toast.makeText(
                        context,
                        "Đăng nhập thất bại. Vui lòng thử lại.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BetterMeColors.BackGround.BackgroundLightBlue),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ===== COVER IMAGE AREA =====
            // TODO: Thêm ảnh bìa vào đây

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .background(BetterMeColors.BackGround.BackgroundLightBlue),
                contentAlignment = Alignment.TopCenter
            ) {
                // Placeholder - bạn sẽ thay bằng Image sau
                Image(
                    painter = painterResource(id = R.drawable.img_login_page),
                    contentDescription = "Cover Image",
                    modifier = Modifier.fillMaxWidth().padding(top = 72.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // ===== BOTTOM SECTION =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .weight(0.5f)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(16.dp))

                // Motivational text
                Text(
                    text = stringResource(R.string.sign_in_motivation),
                    style = BetterMeTypography.Body.Large.Regular,
                    color = BetterMeColors.Text.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ===== GOOGLE SIGN-IN BUTTON =====
                OutlinedButton(
                    onClick = {
                        viewModel.processIntent(SignInIntent.SignInWithGoogle(activity))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.dp, BetterMeColors.Border.BorderLight),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = BetterMeColors.White
                    ),
                    enabled = !state.isLoading
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // TODO: Thay bằng icon Google thật
                         Icon(
                             painter = painterResource(id = R.drawable.ic_google),
                             contentDescription = "Google",
                             modifier = Modifier.size(20.dp),
                             tint = Color.Unspecified
                         )

                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.sign_in_google),
                            style = BetterMeTypography.Title.Medium.SemiBold,
                            color = BetterMeColors.Text.TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ===== SKIP LOGIN LINK =====
                Text(
                    text = stringResource(R.string.sign_in_skip),
                    style = BetterMeTypography.Title.Small.Medium,
                    color = BetterMeColors.Text.TextTertiary,
                    modifier = Modifier
                        .clickable(enabled = !state.isLoading) {
                            viewModel.processIntent(SignInIntent.SkipSignIn)
                        }
                        .padding(8.dp)
                )
            }
        }

        // ===== LOADING OVERLAY =====
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = BetterMeColors.Primary.Primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}