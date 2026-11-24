package com.example.ui.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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

data class ArtistData(val name: String, val listeners: String, val imageUrl: String)

@Composable
fun ArtistScreen(viewModel: MusicViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val artists = listOf(
        ArtistData("The Midnight", "1.2M Monthly Transmissions", "https://lh3.googleusercontent.com/aida-public/AB6AXuDqdCMr_-ekSmUoVVPk1cHSB1ll6kp_NQLJFI7kiw_IXENnxXFC_RUrW4mkaf1NtHvFEq4dTPYyMhau-Pnf0F7d5n_Ub4CMsZ-JVITcgaL5BMC46mUYM7psWsHbqV4XOuysvL_hJ8tKdcM0MOa9KbMTc_9nm_Nv4A7_UDx4GWYmjzey20ActLZZ11MwtsIVKBmhJihUBJylSOsJHddm1YJPlzPy-6jlVLk8qAnxNibGw58uJqVb9OKdY8FQzSROWIM0emiGU2setFX_"),
        ArtistData("Gunship", "890k Monthly Transmissions", "https://lh3.googleusercontent.com/aida-public/AB6AXuAUpFNWGTcVOrzXC-2iwWBlLbKyORUaJ5NxuJRr9ATSpaLTNN-el0XQwcePmkDNfsV0wRhp3F3U_jeSKjvbnKp67snBvZCVqiV8cqopjO3_vkkwbtbudeZJPlHkiTqYi4p8BQ7kLjV7fEo5km5pIanS6_JCtJ6PlTj9bU1-MTQ8tewixigM5LvZbbX2ic0UNdxGKr8dLPVb4oJ896A0_V8ovoSP6iiRJADMWzouIh5AeGZryV7caIX8uXKQLcvuBda2ssdUaFdW21Qr"),
        ArtistData("FM-84", "650k Monthly Transmissions", "https://lh3.googleusercontent.com/aida-public/AB6AXuCKXXN6oN2DLv-Ikct-IdyH9Q8jqH4HacTzCZXkprcFW8rqwypnt0WWJXEmYNEChITfinBPNMmHupitl3I4Ix-Dk3wlLi2eRw3oAx13I4imd_OetKCk5cQ2Namhujky6WwzVzWRRc-OfLX9u-BONfeB1ho_CAM_UrzccWi8wDcLX_kx6Z8jYdkHQmiCVUwz4E5e9ZRqaORa-H6KNSukNIeyKfgscnaqypw8Zzr0kvF8dXIfVW_BPsGxaQNKDvCERbatvK_ekame7bQ7"),
        ArtistData("Lazerhawk", "420k Monthly Transmissions", "https://lh3.googleusercontent.com/aida-public/AB6AXuCCnF7asptZ_a4uC50gs_0zs_DWyyq1pt6Zz836XBsIn72VRSpvW9pDAv5__Dr5uWHwB4qJ-DhNb-tKYX8tE2sVgp9bggv12880y9qM1T9SWuHXLQc8NjnV_H8rXen8J6lcG9vCbmhx1jI8grSVcfhyoWkXJHdFQovRj9QGFRcZPtn32jU_31S6BmZ-Gqp-ikmZQBcuAsRjwGS67BmXO-nSS4WbF_8WT9U592ZrsSetPAqVSQN2VMRzY9ohaJDTZhRlMVlA1mHJsZOd")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(artists) { artist ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = artist.imageUrl,
                        contentDescription = artist.name,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(artist.name, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(artist.listeners, style = MaterialTheme.typography.bodySmall, color = TextGray)
                    }
                    IconButton(onClick = { 
                        viewModel.downloadManager.startDownload("https://soundcloud.com/artist/${artist.name}", "mp3", "320kbps")
                    }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Stream Node", tint = CyberCyan)
                    }
                }
            }
        }
    }
}
