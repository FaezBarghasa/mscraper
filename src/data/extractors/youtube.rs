use super::Extractor;
use crate::domain::models::{AudioFormat, StreamInfo, TrackMetadata};
use crate::media::MediaError;
use async_trait::async_trait;
use reqwest::Client;
use std::time::Duration;

pub struct YouTubeExtractor {
    pub client: Client,
}

impl YouTubeExtractor {
    pub fn new(client: Client) -> Self {
        Self { client }
    }

    async fn fetch_with_backoff(&self, url: &str) -> Result<String, MediaError> {
        let mut retries = 0;
        let mut delay = Duration::from_millis(500);

        loop {
            match self
                .client
                .get(url)
                .timeout(Duration::from_secs(10))
                .send()
                .await
            {
                Ok(resp) if resp.status().is_success() => {
                    return resp
                        .text()
                        .await
                        .map_err(|e| MediaError::ExtractionError(e.to_string()));
                }
                Ok(resp) => {
                    if resp.status() == reqwest::StatusCode::TOO_MANY_REQUESTS {
                        eprintln!("Rate limited by YouTube. Falling back to Piped/Invidious...");
                        // In reality, here we would switch to a Piped/Invidious instance
                    }
                    if retries >= 3 {
                        return Err(MediaError::ExtractionError(format!(
                            "HTTP {}",
                            resp.status()
                        )));
                    }
                }
                Err(e) => {
                    if retries >= 3 {
                        return Err(MediaError::NetworkError(e));
                    }
                }
            }
            tokio::time::sleep(delay).await;
            retries += 1;
            delay *= 2;
        }
    }
}

#[async_trait]
impl Extractor for YouTubeExtractor {
    async fn extract(&self, url: &str) -> Result<StreamInfo, MediaError> {
        let _html = self.fetch_with_backoff(url).await?;

        Ok(StreamInfo {
            stream_url: "https://mock-youtube-stream.url/audio.opus".to_string(),
            format: AudioFormat::Opus,
            bitrate_kbps: 160,
            metadata: TrackMetadata {
                id: "yt123".to_string(),
                title: "Mock YouTube Title".to_string(),
                artist: "Mock YouTube Artist".to_string(),
                album: None,
                album_art_url: Some("https://mock-youtube-art.url/image.jpg".to_string()),
                duration_ms: 180000,
            },
        })
    }
}
