package com.example.betterme.presentation.components.button

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.example.betterme.R
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeShapes
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun DriverXyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = BetterMeShapes.extraLarge,
    containerColor: Color = BetterMeColors.Primary.Primary,
    text: String = stringResource(R.string.get_started),
    style: TextStyle = BetterMeTypography.Title.Medium.SemiBold.copy(
        color = Color.White
    ),
    isFillMaxWidth: Boolean = true
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor
        ),
        shape = shape,
        modifier = modifier
            .then(
                if (isFillMaxWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                }
            )
    ) {
        Text(
            text = text,
            style = style
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DriverXyButtonPreview() {
    DriverXyButton(
        onClick = {}
    )
}