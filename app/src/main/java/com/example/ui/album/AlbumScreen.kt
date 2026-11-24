package com.example.ui.album

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.TextGray
import com.example.viewmodel.MusicViewModel

data class AlbumData(val title: String, val artist: String, val year: String, val imageUrl: String)

@Composable
fun AlbumScreen(viewModel: MusicViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val albums = listOf(
        AlbumData("Endless Summer", "The Midnight", "2016", "https://lh3.googleusercontent.com/aida-public/AB6AXuDqdCMr_-ekSmUoVVPk1cHSB1ll6kp_NQLJFI7kiw_IXENnxXFC_RUrW4mkaf1NtHvFEq4dTPYyMhau-Pnf0F7d5n_Ub4CMsZ-JVITcgaL5BMC46mUYM7psWsHbqV4XOuysvL_hJ8tKdcM0MOa9KbMTc_9nm_Nv4A7_UDx4GWYmjzey20ActLZZ11MwtsIVKBmhJihUBJylSOsJHddm1YJPlzPy-6jlVLk8qAnxNibGw58uJqVb9OKdY8FQzSROWIM0emiGU2setFX_"),
        AlbumData("Dark All Day", "Gunship", "2018", "https://lh3.googleusercontent.com/aida-public/AB6AXuAUpFNWGTcVOrzXC-2iwWBlLbKyORUaJ5NxuJRr9ATSpaLTNN-el0XQwcePmkDNfsV0wRhp3F3U_jeSKjvbnKp67snBvZCVqiV8cqopjO3_vkkwbtbudeZJPlHkiTqYi4p8BQ7kLjV7fEo5km5pIanS6_JCtJ6PlTj9bU1-MTQ8tewixigM5LvZbbX2ic0UNdxGKr8dLPVb4oJ896A0_V8ovoSP6iiRJADMWzouIh5AeGZryV7caIX8uXKQLcvuBda2ssdUaFdW21Qr"),
        AlbumData("Atlas", "FM-84", "2016", "https://lh3.googleusercontent.com/aida-public/AB6AXuCKXXN6oN2DLv-Ikct-IdyH9Q8jqH4HacTzCZXkprcFW8rqwypnt0WWJXEmYNEChITfinBPNMmHupitl3I4Ix-Dk3wlLi2eRw3oAx13I4imd_OetKCk5cQ2Namhujky6WwzVzWRRc-OfLX9u-BONfeB1ho_CAM_UrzccWi8wDcLX_kx6Z8jYdkHQmiCVUwz4E5e9ZRqaORa-H6KNSukNIeyKfgscnaqypw8Zzr0kvF8dXIfVW_BPsGxaQNKDvCERbatvK_ekame7bQ7"),
        AlbumData("Skull & Shark", "Lazerhawk", "2013", "https://lh3.googleusercontent.com/aida-public/AB6AXuBzHyiMbZ3DrUc4JhhC7d9wW0vw_t-lfV1DtDolywLb9mRYQ4-UOTr49bhgWtylhmQGt-hxIiOyHZEb2-uApQ6f1vNdcuo0l4xPbw0yTuL_ThR0jHGOD9HAmDWQUwur6Ti6eF_ZDIksYeAEyVqCQlx05nxnbfiTFDxgXDfz6GOeBnXOrhhzb6sqjbrDXD8XlfVcuym9RuC9HeE0Ra8oUNfN3jwvO6Ga6myUP5XfMJSlzWMT_-p0M_-XiYmsoqT1AyMMrSFOYkdAtP_G")
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
        items(albums) { album ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    AsyncImage(
                        model = album.imageUrl,
                        contentDescription = album.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(album.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, fontWeight = FontWeight.Bold)
                    Text(album.artist, style = MaterialTheme.typography.bodySmall, color = CyberCyan, maxLines = 1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(album.year, style = MaterialTheme.typography.labelSmall, color = TextGray)
                        
                        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                        val context = androidx.compose.ui.platform.LocalContext.current
                        IconButton(
                            onClick = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.downloadManager.startDownload("https://soundcloud.com/album/${album.title}", "mp3", "320kbps")
                                android.widget.Toast.makeText(context, "Downloading album: ${album.title}", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = "Download Album", tint = CyberCyan)
                        }
                    }
                }
            }
        }
    }
}
