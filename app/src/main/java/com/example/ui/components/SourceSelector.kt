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
import com.example.ui.theme.HoloBg
import com.example.ui.theme.TokyoBlue
import com.example.ui.theme.TokyoPink

@Composable
fun SourceSelector(
    selectedSource: SearchSource,
    onSourceChanged: (SearchSource) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = TokyoBlue
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(HoloBg)
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchSource.entries.forEach { source ->
            val isSelected = source == selectedSource
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { onSourceChanged(source) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = source.displayName.uppercase(),
                    color = if (isSelected) accentColor else Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
