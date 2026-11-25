package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SearchSource
import com.example.ui.theme.*

@Composable
fun SourceSelector(
    selectedSource: SearchSource,
    onSourceChanged: (SearchSource) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = PrimaryAccent
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceElevated)
            .border(1.dp, HairlineBorder, RoundedCornerShape(22.dp))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchSource.entries.forEach { source ->
            val isSelected = source == selectedSource
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) accentColor else Color.Transparent)
                    .clickable { onSourceChanged(source) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = source.displayName,
                    color = if (isSelected) ObsidianBlack else TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
