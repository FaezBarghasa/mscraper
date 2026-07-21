use super::Extractor;
use crate::domain::models::{AudioFormat, StreamInfo, TrackMetadata};
use crate::media::MediaError;
use async_trait::async_trait;
use reqwest::Client;
use std::time::Duration;

pub struct SoundCloudExtractor {
    pub client: Client,
}

impl SoundCloudExtractor {
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
impl Extractor for SoundCloudExtractor {
    async fn extract(&self, url: &str) -> Result<StreamInfo, MediaError> {
        let _html = self.fetch_with_backoff(url).await?;

        Ok(StreamInfo {
            stream_url: "https://mock-sc-stream.url/audio.mp3".to_string(),
            format: AudioFormat::Mp3,
            bitrate_kbps: 128,
            metadata: TrackMetadata {
                id: "sc123".to_string(),
                title: "Mock SC Title".to_string(),
                artist: "Mock SC Artist".to_string(),
                album: None,
                album_art_url: Some("https://mock-sc-art.url/image.jpg".to_string()),
                duration_ms: 210000,
            },
        })
    }
}
