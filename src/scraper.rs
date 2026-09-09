use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct Track {
    pub video_id: String,
    pub title: String,
    pub artists: Vec<String>,
    pub duration_seconds: i32,
    pub cover_url: String,
}

#[derive(Debug, uniffi::Error, thiserror::Error)]
pub enum ScraperError {
    #[error("Network error: {0}")]
    Network(String),
    #[error("Other error: {0}")]
    Other(String),
}

#[derive(uniffi::Object)]
pub struct YtMusicScraper {
    client: reqwest::Client,
}

#[uniffi::export]
impl YtMusicScraper {
    #[uniffi::constructor]
    pub fn new() -> Self {
        Self {
            client: reqwest::Client::new(),
        }
    }

    pub async fn search_tracks(&self, query: String) -> Result<Vec<Track>, ScraperError> {
        // Placeholder for the actual search extraction logic
        // E.g., sending a POST request to https://music.youtube.com/youtubei/v1/search
        Ok(vec![Track {
            video_id: "dQw4w9WgXcQ".to_string(),
            title: format!("Search result for: {}", query),
            artists: vec!["Rick Astley".to_string()],
            duration_seconds: 212,
            cover_url: "https://example.com/cover.jpg".to_string(),
        }])
    }
}
