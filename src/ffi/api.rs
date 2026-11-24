use crate::data::extractors::soundcloud::SoundCloudExtractor;
use crate::data::extractors::youtube::YouTubeExtractor;
use crate::network::quic_client::QuicClient;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use std::sync::{Arc, LazyLock, RwLock};

static GLOBAL_QUIC_CLIENT: LazyLock<Arc<QuicClient>> = LazyLock::new(|| {
    Arc::new(QuicClient::new().expect("Failed to initialize persistent QuicClient"))
});

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum EngineError {
    #[error("Engine error: {0}")]
    Generic(String),
    #[error("IO error: {0}")]
    Io(String),
    #[error("Network error: {0}")]
    Network(String),
    #[error("Extractor error: {0}")]
    Extractor(String),
    #[error("Serialization error: {0}")]
    Serialization(String),
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, uniffi::Enum)]
pub enum AudioSource {
    YouTubeMusic,
    SoundCloud,
    Spotify,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, uniffi::Enum)]
pub enum AudioQuality {
    Low,
    Medium,
    High,
    Lossless,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, uniffi::Enum)]
pub enum AudioFormat {
    Flac,
    Wav,
    Mp3,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct Track {
    pub id: String,
    pub title: String,
    pub artist: String,
    pub album: Option<String>,
    pub source_url: String,
    pub duration: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct TrackMetadata {
    pub title: String,
    pub artist: String,
    pub album: Option<String>,
    pub album_art_url: Option<String>,
    pub track_id: String,
    pub source: AudioSource,
    pub duration_seconds: Option<u64>,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct Playlist {
    pub id: String,
    pub name: String,
    pub description: Option<String>,
    pub tracks: Vec<Track>,
    pub source: AudioSource,
}

#[derive(uniffi::Object)]
pub struct MmDlpApi {
    quic_enabled: RwLock<bool>,
    youtube_extractor: Arc<YouTubeExtractor>,
    soundcloud_extractor: Arc<SoundCloudExtractor>,
}

impl Default for MmDlpApi {
    fn default() -> Self {
        Self::new()
    }
}

#[uniffi::export]
impl MmDlpApi {
    #[uniffi::constructor]
    pub fn new() -> Self {
        Self {
            quic_enabled: RwLock::new(true),
            youtube_extractor: Arc::new(YouTubeExtractor::new()),
            soundcloud_extractor: Arc::new(SoundCloudExtractor::new()),
        }
    }

    pub fn set_network_config(&self, enable_quic: bool) {
        if let Ok(mut quic) = self.quic_enabled.write() {
            *quic = enable_quic;
        }
    }

    pub async fn search(
        &self,
        query: String,
        source: AudioSource,
    ) -> Result<Vec<TrackMetadata>, EngineError> {
        match source {
            AudioSource::YouTubeMusic => self.youtube_extractor.search(&query).await,
            AudioSource::SoundCloud => self.soundcloud_extractor.search(&query).await,
            AudioSource::Spotify => {
                // Route Spotify search queries via YouTube Music catalogue
                self.youtube_extractor.search(&query).await
            }
        }
    }

    pub async fn get_stream_url(
        &self,
        track_id: String,
        source: AudioSource,
    ) -> Result<String, EngineError> {
        match source {
            AudioSource::YouTubeMusic | AudioSource::Spotify => {
                let (_meta, stream_url) =
                    self.youtube_extractor.extract_video_info(&track_id).await?;
                Ok(stream_url)
            }
            AudioSource::SoundCloud => self.soundcloud_extractor.get_stream_url(&track_id).await,
        }
    }

    pub async fn extract_metadata(&self, url: String) -> Result<TrackMetadata, EngineError> {
        if url.contains("soundcloud.com") {
            let results = self.soundcloud_extractor.search(&url).await?;
            results.into_iter().next().ok_or_else(|| {
                EngineError::Extractor("No metadata found for SoundCloud track".to_string())
            })
        } else {
            let video_id = if let Some(idx) = url.find("v=") {
                let rest = &url[idx + 2..];
                rest.split('&').next().unwrap_or(rest).to_string()
            } else if let Some(idx) = url.find("youtu.be/") {
                let rest = &url[idx + 9..];
                rest.split('?').next().unwrap_or(rest).to_string()
            } else {
                url.clone()
            };

            let (metadata, _stream) = self.youtube_extractor.extract_video_info(&video_id).await?;
            Ok(metadata)
        }
    }

    pub async fn download_track(
        &self,
        url: String,
        _quality: AudioQuality,
        format: AudioFormat,
        temp_dir: String,
    ) -> Result<String, EngineError> {
        let enable_quic = *self.quic_enabled.read().unwrap_or_else(|e| e.into_inner());

        // Resolve stream URL and canonical track ID
        let (canonical_id, stream_url) = if url.contains("youtube.com") || url.contains("youtu.be")
        {
            let video_id = if let Some(idx) = url.find("v=") {
                let rest = &url[idx + 2..];
                rest.split('&').next().unwrap_or(rest).to_string()
            } else if let Some(idx) = url.find("youtu.be/") {
                let rest = &url[idx + 9..];
                rest.split('?').next().unwrap_or(rest).to_string()
            } else {
                url.clone()
            };
            let stream = self
                .youtube_extractor
                .extract_video_info(&video_id)
                .await
                .map(|(_meta, s)| s)
                .unwrap_or_else(|_| url.clone());
            (video_id, stream)
        } else {
            use std::hash::{DefaultHasher, Hasher};
            let mut hasher = DefaultHasher::new();
            hasher.write(url.as_bytes());
            let raw_hash = format!("{:016x}", hasher.finish());
            (raw_hash, url)
        };

        // Determine realistic container extension from stream or requested format
        let ext = if stream_url.contains("mime=audio%2Fwebm") || stream_url.contains("audio/webm") {
            "webm"
        } else if stream_url.contains("mime=audio%2Fmp4") || stream_url.contains("audio/mp4") {
            "m4a"
        } else {
            match format {
                AudioFormat::Flac => "flac",
                AudioFormat::Wav => "wav",
                AudioFormat::Mp3 => "mp3",
            }
        };

        let sanitized_id: String = canonical_id
            .chars()
            .map(|c| {
                if c.is_alphanumeric() || c == '-' || c == '_' {
                    c
                } else {
                    '_'
                }
            })
            .collect();
        let file_name = format!("track_{}.{}", sanitized_id, ext);
        let file_path = PathBuf::from(temp_dir).join(&file_name);

        GLOBAL_QUIC_CLIENT
            .stream_to_file(&stream_url, &file_path, enable_quic)
            .await
            .map_err(|e| EngineError::Network(format!("Failed to stream audio payload: {e}")))?;

        Ok(file_path.to_string_lossy().to_string())
    }

    pub fn export_playlist_json(&self, playlist: Playlist) -> Result<String, EngineError> {
        serde_json::to_string_pretty(&playlist)
            .map_err(|e| EngineError::Serialization(e.to_string()))
    }

    pub fn export_playlist_xml(&self, playlist: Playlist) -> Result<String, EngineError> {
        let mut xml = String::from("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<playlist>\n");
        xml.push_str(&format!("  <id>{}</id>\n", playlist.id));
        xml.push_str(&format!("  <name>{}</name>\n", playlist.name));
        if let Some(desc) = &playlist.description {
            xml.push_str(&format!("  <description>{}</description>\n", desc));
        }
        xml.push_str("  <tracks>\n");
        for track in &playlist.tracks {
            xml.push_str("    <track>\n");
            xml.push_str(&format!("      <id>{}</id>\n", track.id));
            xml.push_str(&format!("      <title>{}</title>\n", track.title));
            xml.push_str(&format!("      <artist>{}</artist>\n", track.artist));
            if let Some(album) = &track.album {
                xml.push_str(&format!("      <album>{}</album>\n", album));
            }
            xml.push_str(&format!(
                "      <source_url>{}</source_url>\n",
                track.source_url
            ));
            xml.push_str(&format!("      <duration>{}</duration>\n", track.duration));
            xml.push_str("    </track>\n");
        }
        xml.push_str("  </tracks>\n</playlist>\n");
        Ok(xml)
    }

    pub fn import_playlist_json(&self, json: String) -> Result<Playlist, EngineError> {
        serde_json::from_str(&json).map_err(|e| EngineError::Serialization(e.to_string()))
    }
}
