use crate::data::extractors::soundcloud::SoundCloudExtractor;
use crate::data::extractors::youtube::YouTubeExtractor;
use crate::network::quic_client::QuicClient;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use std::sync::RwLock;

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
        }
    }

    pub fn set_network_config(&self, enable_quic: bool) {
        if let Ok(mut quic) = self.quic_enabled.write() {
            *quic = enable_quic;
        }
    }

    pub fn search(
        &self,
        query: String,
        source: AudioSource,
    ) -> Result<Vec<TrackMetadata>, EngineError> {
        let rt = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .map_err(|e| EngineError::Generic(e.to_string()))?;

        rt.block_on(async {
            match source {
                AudioSource::YouTubeMusic => {
                    let extractor = YouTubeExtractor::new();
                    extractor.search(&query).await
                }
                AudioSource::SoundCloud => {
                    let extractor = SoundCloudExtractor::new();
                    extractor.search(&query).await
                }
                AudioSource::Spotify => {
                    // Route Spotify search queries via YouTube Music catalogue
                    let extractor = YouTubeExtractor::new();
                    extractor.search(&query).await
                }
            }
        })
    }

    pub fn get_stream_url(
        &self,
        track_id: String,
        source: AudioSource,
    ) -> Result<String, EngineError> {
        let rt = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .map_err(|e| EngineError::Generic(e.to_string()))?;

        rt.block_on(async {
            match source {
                AudioSource::YouTubeMusic | AudioSource::Spotify => {
                    let extractor = YouTubeExtractor::new();
                    let (_meta, stream_url) = extractor.extract_video_info(&track_id).await?;
                    Ok(stream_url)
                }
                AudioSource::SoundCloud => {
                    let extractor = SoundCloudExtractor::new();
                    extractor.get_stream_url(&track_id).await
                }
            }
        })
    }

    pub fn extract_metadata(&self, url: String) -> Result<TrackMetadata, EngineError> {
        let rt = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .map_err(|e| EngineError::Generic(e.to_string()))?;

        rt.block_on(async {
            if url.contains("soundcloud.com") {
                let extractor = SoundCloudExtractor::new();
                let results = extractor.search(&url).await?;
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

                let extractor = YouTubeExtractor::new();
                let (metadata, _stream) = extractor.extract_video_info(&video_id).await?;
                Ok(metadata)
            }
        })
    }

    pub fn download_track(
        &self,
        url: String,
        _quality: AudioQuality,
        format: AudioFormat,
        temp_dir: String,
    ) -> Result<String, EngineError> {
        let ext = match format {
            AudioFormat::Flac => "flac",
            AudioFormat::Wav => "wav",
            AudioFormat::Mp3 => "mp3",
        };

        let file_name = format!(
            "track_{}.{}",
            std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .map(|d| d.as_millis())
                .unwrap_or(0),
            ext
        );
        let file_path = PathBuf::from(temp_dir).join(&file_name);

        let enable_quic = *self.quic_enabled.read().unwrap_or_else(|e| e.into_inner());

        let rt = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .map_err(|e| EngineError::Generic(e.to_string()))?;

        // Resolve stream URL if input is a YouTube/SoundCloud webpage URL
        let stream_url = rt.block_on(async {
            if url.contains("youtube.com") || url.contains("youtu.be") {
                let video_id = if let Some(idx) = url.find("v=") {
                    let rest = &url[idx + 2..];
                    rest.split('&').next().unwrap_or(rest).to_string()
                } else if let Some(idx) = url.find("youtu.be/") {
                    let rest = &url[idx + 9..];
                    rest.split('?').next().unwrap_or(rest).to_string()
                } else {
                    url.clone()
                };
                let extractor = YouTubeExtractor::new();
                extractor
                    .extract_video_info(&video_id)
                    .await
                    .map(|(_meta, stream)| stream)
                    .unwrap_or(url)
            } else {
                url
            }
        });

        let fetched_bytes = rt.block_on(async {
            let client = QuicClient::new().map_err(|e| EngineError::Network(e.to_string()))?;
            client
                .fetch(&stream_url, enable_quic)
                .await
                .map_err(|e| EngineError::Network(format!("Failed to stream audio payload: {e}")))
        })?;

        std::fs::write(&file_path, fetched_bytes).map_err(|e| EngineError::Io(e.to_string()))?;

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
            xml.push_str(&format!("      <source_url>{}</source_url>\n", track.source_url));
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
