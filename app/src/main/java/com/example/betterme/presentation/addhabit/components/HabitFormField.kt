package com.example.betterme.presentation.addhabit.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

@Composable
fun HabitFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    isError: Boolean = false,
    errorText: String? = null,
    containerColor: androidx.compose.ui.graphics.Color = BetterMeColors.BackGround.BackgroundPrimary,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = BetterMeTypography.Body.Small.Medium,
            color = BetterMeColors.Text.TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = BetterMeTypography.Body.Medium,
                    color = BetterMeColors.Text.TextTertiary
                )
            },
            singleLine = singleLine,
            minLines = minLines,
            isError = isError,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BetterMeColors.Primary.Primary,
                unfocusedBorderColor = BetterMeColors.Border.BorderLight,
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                cursorColor = BetterMeColors.Primary.Primary,
                errorBorderColor = BetterMeColors.Red
            )
        )
        if (isError && errorText != null) {
            Text(
                text = errorText,
                style = BetterMeTypography.Body.Small.Medium,
                color = BetterMeColors.Red,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}
