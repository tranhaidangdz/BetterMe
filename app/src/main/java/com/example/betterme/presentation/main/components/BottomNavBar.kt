package com.example.betterme.presentation.main.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.betterme.presentation.main.model.MainTab
import com.example.betterme.presentation.theme.BetterMeColors
import com.example.betterme.presentation.theme.BetterMeTypography

/**
 * Custom Shape tạo hiệu ứng lõm (notch) ở giữa top của bottom bar
 */
class BottomBarCutoutShape(
    private val cutoutRadius: Dp = 38.dp,
    private val cutoutMargin: Dp = 8.dp,
    private val cornerRadius: Dp = 0.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cutoutRadiusPx = with(density) { cutoutRadius.toPx() }
        val cutoutMarginPx = with(density) { cutoutMargin.toPx() }
        val totalCutoutRadius = cutoutRadiusPx + cutoutMarginPx

        val centerX = size.width / 2f

        val path = Path().apply {
            // Bắt đầu từ góc trái trên
            moveTo(0f, 0f)

            // Đi tới điểm bắt đầu curve bên trái
            lineTo(centerX - totalCutoutRadius - cutoutRadiusPx * 0.5f, 0f)

            // Curve lõm vào (dùng cubic bezier)
            cubicTo(
                x1 = centerX - totalCutoutRadius * 0.6f,
                y1 = 0f,
                x2 = centerX - totalCutoutRadius * 0.5f,
                y2 = totalCutoutRadius * 0.75f,
                x3 = centerX,
                y3 = totalCutoutRadius * 0.75f
            )
            cubicTo(
                x1 = centerX + totalCutoutRadius * 0.5f,
                y1 = totalCutoutRadius * 0.75f,
                x2 = centerX + totalCutoutRadius * 0.6f,
                y2 = 0f,
                x3 = centerX + totalCutoutRadius + cutoutRadiusPx * 0.5f,
                y3 = 0f
            )

            // Đi tới góc phải trên
            lineTo(size.width, 0f)

            // Đi xuống dưới
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun BottomNavBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // Background bar với cutout shape
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .shadow(
                    elevation = 16.dp,
                    shape = BottomBarCutoutShape()
                )
                .clip(BottomBarCutoutShape())
                .background(BetterMeColors.TabBar.TabBarBackground)
                .padding(horizontal = 8.dp)
                .padding(top = 12.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainTab.entries.forEach { tab ->
                    if (tab == MainTab.ADD) {
                        // Spacer cho vị trí FAB
                        Spacer(modifier = Modifier.width(56.dp))
                    } else {
                        BottomNavItem(
                            tab = tab,
                            isSelected = tab == selectedTab,
                            onClick = { onTabSelected(tab) }
                        )
                    }
                }
            }
        }

        // FAB nổi ở giữa, cao hơn nav bar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-24).dp)
                .size(60.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(BetterMeColors.TabBar.TabBarFabBackground)
                .clickable { onTabSelected(MainTab.ADD) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(MainTab.ADD.iconRes),
                contentDescription = MainTab.ADD.label,
                tint = BetterMeColors.TabBar.TabBarFabIcon,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun BottomNavItem(tab: MainTab, isSelected: Boolean, onClick: () -> Unit) {
    val tintColor by animateColorAsState(
        targetValue = if (isSelected) BetterMeColors.TabBar.TabBarSelectedText
        else BetterMeColors.TabBar.TabBarUnselectedText,
        animationSpec = tween(200), label = "tint"
    )

    Column(
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(tab.iconRes),
            contentDescription = tab.label,
            tint = tintColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tab.label,
            style = BetterMeTypography.Body.Small.Medium.copy(fontSize = 10.sp),
            color = tintColor
        )
    }
}
