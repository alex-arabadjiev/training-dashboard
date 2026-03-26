package com.example.trainingdashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trainingdashboard.ui.theme.KineticGreen
import com.example.trainingdashboard.ui.theme.KineticOnSurface
import com.example.trainingdashboard.ui.theme.KineticSurfaceContainerHigh

@Composable
fun DayHeader(
    dayNumber: Int,
    goalLevel: Int,
    progressPercent: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Goal level chip
        Box(
            modifier = Modifier
                .background(
                    color = KineticSurfaceContainerHigh,
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "LEVEL $goalLevel",
                style = MaterialTheme.typography.labelSmall,
                color = KineticGreen
            )
        }

        // Day number + progress percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "DAY $dayNumber",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontStyle = FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "$progressPercent%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = KineticGreen
                )
                Text(
                    text = "TOTAL PROGRESS",
                    style = MaterialTheme.typography.labelSmall,
                    color = KineticOnSurface
                )
            }
        }
    }
}
