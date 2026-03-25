package com.example.trainingdashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trainingdashboard.ui.theme.KineticGreen
import com.example.trainingdashboard.ui.theme.KineticSurfaceContainer
import com.example.trainingdashboard.ui.theme.KineticOnSurfaceVariant

@Composable
fun CompletionBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KineticSurfaceContainer)
            .padding(32.dp)
    ) {
        // Background watermark
        Text(
            text = "WIN",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                fontSize = 96.sp,
                letterSpacing = (-2).sp
            ),
            color = KineticGreen.copy(alpha = 0.05f),
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        // Main content
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(1.dp)
                    .background(KineticGreen.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "PRECISION",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    fontSize = 36.sp,
                    lineHeight = 32.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "IN EVERY REP",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    fontSize = 36.sp,
                    lineHeight = 32.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = KineticGreen
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "EXCELLENCE IS A HABIT",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 4.sp
                ),
                color = KineticOnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(1.dp)
                    .background(KineticGreen.copy(alpha = 0.3f))
            )
        }
    }
}
