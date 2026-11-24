package com.example.ui.genres

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepVoid

data class GenreData(val name: String, val streams: String, val gradient: Brush)

@Composable
fun GenresScreen() {
    val genres = listOf(
        GenreData("SYNTHWAVE", "852k Active Streams", Brush.horizontalGradient(listOf(Color(0xFF00FFFF), Color(0xFFFF00FF)))),
        GenreData("INDUSTRIAL", "610k Active Streams", Brush.horizontalGradient(listOf(Color(0xFFFF00FF), Color(0xFFFF8800)))),
        GenreData("ELECTRO-DARK", "431k Active Streams", Brush.horizontalGradient(listOf(Color(0xFFFF3366), Color(0xFF330066)))),
        GenreData("OUTRUN", "295k Active Streams", Brush.horizontalGradient(listOf(Color(0xFF00FF66), Color(0xFF0099FF)))),
        GenreData("VAPORWAVE", "180k Active Streams", Brush.horizontalGradient(listOf(Color(0xFFFFCCFF), Color(0xFFCCFFFF)))),
        GenreData("CYBER-GOTH", "142k Active Streams", Brush.horizontalGradient(listOf(Color(0xFF660099), Color(0xFF000000))))
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(genres) { genre ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(genre.gradient)
                    .clickable { }
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text(genre.name, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(genre.streams, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}
