package com.example.trainingdashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trainingdashboard.BuildConfig
import com.example.trainingdashboard.ui.theme.KineticBackground
import com.example.trainingdashboard.ui.theme.KineticGreen
import com.example.trainingdashboard.ui.theme.KineticOnSurfaceVariant
import com.example.trainingdashboard.ui.theme.KineticOutline

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        KineticBackground,
                        KineticBackground.copy(alpha = 0.92f),
                        KineticBackground
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle radial glow behind the wordmark
        Box(
            modifier = Modifier
                .size(320.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                KineticGreen.copy(alpha = 0.06f),
                                KineticGreen.copy(alpha = 0f)
                            )
                        )
                    )
                }
        )

        // Center content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Wordmark
            Text(
                text = "KINETIC",
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = (-2).sp,
                color = KineticGreen
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tagline
            Text(
                text = "MOMENTUM COMPOUNDS",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 6.sp,
                color = KineticOnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Technical line with flanking rules
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(1.dp)
                        .background(KineticOutline.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "v${BuildConfig.VERSION_NAME} :: MOMENTUM.INIT",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = KineticOnSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(1.dp)
                        .background(KineticOutline.copy(alpha = 0.4f))
                )
            }
        }

        // Corner accents
        CornerAccent(Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 24.dp), topLeft = true)
        CornerAccent(Modifier.align(Alignment.TopEnd).padding(end = 24.dp, top = 24.dp), topRight = true)
        CornerAccent(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 24.dp), bottomLeft = true)
        CornerAccent(Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 24.dp), bottomRight = true)
    }
}

@Composable
private fun CornerAccent(
    modifier: Modifier = Modifier,
    topLeft: Boolean = false,
    topRight: Boolean = false,
    bottomLeft: Boolean = false,
    bottomRight: Boolean = false
) {
    val color = KineticOutline.copy(alpha = 0.4f)
    val size = 20.dp
    val stroke = 1.dp

    Box(modifier = modifier.size(size)) {
        if (topLeft || bottomLeft) {
            Box(
                Modifier
                    .width(stroke)
                    .height(size)
                    .align(Alignment.CenterStart)
                    .background(color)
            )
        }
        if (topRight || bottomRight) {
            Box(
                Modifier
                    .width(stroke)
                    .height(size)
                    .align(Alignment.CenterEnd)
                    .background(color)
            )
        }
        if (topLeft || topRight) {
            Box(
                Modifier
                    .width(size)
                    .height(stroke)
                    .align(Alignment.TopCenter)
                    .background(color)
            )
        }
        if (bottomLeft || bottomRight) {
            Box(
                Modifier
                    .width(size)
                    .height(stroke)
                    .align(Alignment.BottomCenter)
                    .background(color)
            )
        }
    }
}
