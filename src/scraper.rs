use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ScrapedTrack {
    pub video_id: String,
    pub title: String,
    pub artists: Vec<String>,
    pub duration_seconds: i32,
    pub cover_url: String,
}

#[derive(Debug, thiserror::Error)]
pub enum ScraperError {
    #[error("Network error: {0}")]
    Network(String),
    #[error("Other error: {0}")]
    Other(String),
}

pub struct YtMusicScraper {
    _client: reqwest::Client,
}

impl YtMusicScraper {
    pub fn new() -> Self {
        Self {
            _client: reqwest::Client::new(),
        }
    }

    pub async fn search_tracks(&self, query: String) -> Result<Vec<ScrapedTrack>, ScraperError> {
        Ok(vec![ScrapedTrack {
            video_id: "dQw4w9WgXcQ".to_string(),
            title: format!("Search result for: {}", query),
            artists: vec!["Rick Astley".to_string()],
            duration_seconds: 212,
            cover_url: "https://example.com/cover.jpg".to_string(),
        }])
    }
}

impl Default for YtMusicScraper {
    fn default() -> Self {
        Self::new()
    }
}
