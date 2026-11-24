use crate::ffi::api::{AudioSource, EngineError, TrackMetadata};
use regex::Regex;
use reqwest::Client;
use serde_json::Value;
use std::sync::Arc;
use std::time::Duration;
use tokio::sync::RwLock;

pub struct SoundCloudExtractor {
    client: Client,
    client_id: Arc<RwLock<Option<String>>>,
}

impl Default for SoundCloudExtractor {
    fn default() -> Self {
        Self::new()
    }
}

impl SoundCloudExtractor {
    pub fn new() -> Self {
        let client = Client::builder()
            .timeout(Duration::from_secs(15))
            .user_agent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
            .unwrap_or_default();
        Self {
            client,
            client_id: Arc::new(RwLock::new(None)),
        }
    }

    async fn get_client_id(&self) -> Result<String, EngineError> {
        {
            let guard = self.client_id.read().await;
            if let Some(ref id) = *guard {
                return Ok(id.clone());
            }
        }

        let home_html = self
            .client
            .get("https://soundcloud.com")
            .send()
            .await
            .map_err(|e| EngineError::Network(format!("Failed to reach SoundCloud: {e}")))?
            .text()
            .await
            .map_err(|e| EngineError::Network(format!("Failed to read SoundCloud HTML: {e}")))?;

        let script_regex = Regex::new(
            r#"<script crossorigin src="(https://a-v2\.sndcdn\.com/assets/[^"]+\.js)"></script>"#,
        )
        .map_err(|e| EngineError::Extractor(e.to_string()))?;

        let client_id_regex = Regex::new(r#",client_id:"([a-zA-Z0-9]+)""#)
            .map_err(|e| EngineError::Extractor(e.to_string()))?;

        for cap in script_regex.captures_iter(&home_html) {
            if let Some(script_url) = cap.get(1).map(|m| m.as_str()) {
                if let Ok(resp) = self.client.get(script_url).send().await {
                    if let Ok(js_code) = resp.text().await {
                        if let Some(id_cap) = client_id_regex.captures(&js_code) {
                            if let Some(id) = id_cap.get(1).map(|m| m.as_str().to_string()) {
                                let mut guard = self.client_id.write().await;
                                *guard = Some(id.clone());
                                return Ok(id);
                            }
                        }
                    }
                }
            }
        }

        // Fallback default client id if dynamic scrape fails
        let fallback_id = "iY79tftuo72yUK1mt9US1vStt4xSmEO3".to_string();
        let mut guard = self.client_id.write().await;
        *guard = Some(fallback_id.clone());
        Ok(fallback_id)
    }

    pub async fn search(&self, query: &str) -> Result<Vec<TrackMetadata>, EngineError> {
        let client_id = self.get_client_id().await?;
        let search_url = format!(
            "https://api-v2.soundcloud.com/search/tracks?q={}&client_id={}&limit=20",
            urlencoding::encode(query),
            client_id
        );

        let resp = self
            .client
            .get(&search_url)
            .send()
            .await
            .map_err(|e| EngineError::Network(format!("SoundCloud search request failed: {e}")))?;

        let json: Value = resp
            .json()
            .await
            .map_err(|e| EngineError::Extractor(format!("Failed to parse SoundCloud search response: {e}")))?;

        let mut tracks = Vec::new();
        if let Some(collection) = json["collection"].as_array() {
            for item in collection {
                let track_id = match item["id"].as_u64() {
                    Some(id) => id.to_string(),
                    None => continue,
                };

                let title = item["title"].as_str().unwrap_or("Unknown Title").to_string();
                let artist = item["user"]["username"]
                    .as_str()
                    .unwrap_or("Unknown Artist")
                    .to_string();
                let album_art_url = item["artwork_url"].as_str().map(str::to_string);

                tracks.push(TrackMetadata {
                    title,
                    artist,
                    album: None,
                    album_art_url,
                    track_id,
                    source: AudioSource::SoundCloud,
                });
            }
        }

        Ok(tracks)
    }

    pub async fn get_stream_url(&self, track_id: &str) -> Result<String, EngineError> {
        let client_id = self.get_client_id().await?;
        let track_url = format!(
            "https://api-v2.soundcloud.com/tracks/{}?client_id={}",
            track_id, client_id
        );

        let resp = self
            .client
            .get(&track_url)
            .send()
            .await
            .map_err(|e| EngineError::Network(format!("SoundCloud track lookup failed: {e}")))?;

        let json: Value = resp
            .json()
            .await
            .map_err(|e| EngineError::Extractor(format!("Failed to parse SoundCloud track JSON: {e}")))?;

        let media = &json["media"]["transcodings"];
        if let Some(transcodings) = media.as_array() {
            for t in transcodings {
                let format_protocol = t["format"]["protocol"].as_str().unwrap_or_default();
                if format_protocol == "progressive" || format_protocol == "hls" {
                    if let Some(stream_endpoint) = t["url"].as_str() {
                        let stream_auth_url = format!("{}?client_id={}", stream_endpoint, client_id);
                        if let Ok(stream_resp) = self.client.get(&stream_auth_url).send().await {
                            if let Ok(stream_json) = stream_resp.json::<Value>().await {
                                if let Some(final_url) = stream_json["url"].as_str() {
                                    return Ok(final_url.to_string());
                                }
                            }
                        }
                    }
                }
            }
        }

        Err(EngineError::Extractor("No playable stream URL found for SoundCloud track".to_string()))
    }
}
