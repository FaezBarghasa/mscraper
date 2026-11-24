use crate::ffi::api::{AudioSource, EngineError, TrackMetadata};
use reqwest::Client;
use serde_json::Value;
use std::time::Duration;

pub struct YouTubeExtractor {
    client: Client,
}

impl Default for YouTubeExtractor {
    fn default() -> Self {
        Self::new()
    }
}

impl YouTubeExtractor {
    pub fn new() -> Self {
        let client = Client::builder()
            .timeout(Duration::from_secs(15))
            .user_agent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
            .unwrap_or_default();
        Self { client }
    }

    fn build_android_context() -> Value {
        serde_json::json!({
            "client": {
                "clientName": "ANDROID",
                "clientVersion": "19.29.35",
                "androidSdkVersion": 30,
                "userAgent": "com.google.android.youtube/19.29.35 (Linux; U; Android 11) gzip",
                "hl": "en",
                "gl": "US"
            }
        })
    }

    pub async fn search(&self, query: &str) -> Result<Vec<TrackMetadata>, EngineError> {
        let body = serde_json::json!({
            "context": {
                "client": {
                    "clientName": "WEB_REMIX",
                    "clientVersion": "1.20240707.12.00",
                    "hl": "en",
                    "gl": "US"
                }
            },
            "query": query
        });

        let resp = self
            .client
            .post("https://music.youtube.com/youtubei/v1/search?alt=json")
            .header("Content-Type", "application/json")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", "1.20240707.12.00")
            .json(&body)
            .send()
            .await
            .map_err(|e| EngineError::Network(format!("InnerTube search request failed: {e}")))?;

        let json: Value = resp
            .json()
            .await
            .map_err(|e| EngineError::Extractor(format!("Failed to parse search response: {e}")))?;

        let mut tracks = Vec::new();

        // Navigate tabbedSearchResultsRenderer
        let tabs = json["contents"]["tabbedSearchResultsRenderer"]["tabs"]
            .as_array()
            .cloned()
            .unwrap_or_default();

        for tab in &tabs {
            let contents = tab["tabRenderer"]["content"]["sectionListRenderer"]["contents"]
                .as_array()
                .cloned()
                .unwrap_or_default();

            for section in &contents {
                let items = section["musicShelfRenderer"]["contents"]
                    .as_array()
                    .cloned()
                    .unwrap_or_default();

                for item in &items {
                    let renderer = &item["musicResponsiveListItemRenderer"];

                    let track_id = renderer["playlistItemData"]["videoId"]
                        .as_str()
                        .unwrap_or("")
                        .to_string();
                    if track_id.is_empty() {
                        continue;
                    }

                    let title = renderer["flexColumns"][0]
                        ["musicResponsiveListItemFlexColumnRenderer"]["text"]["runs"][0]["text"]
                        .as_str()
                        .unwrap_or("Unknown Title")
                        .to_string();

                    let artist = renderer["flexColumns"][1]
                        ["musicResponsiveListItemFlexColumnRenderer"]["text"]["runs"][0]["text"]
                        .as_str()
                        .unwrap_or("Unknown Artist")
                        .to_string();

                    let album_art_url = renderer["thumbnail"]["musicThumbnailRenderer"]
                        ["thumbnail"]["thumbnails"]
                        .as_array()
                        .and_then(|arr| arr.last())
                        .and_then(|t| t["url"].as_str())
                        .map(str::to_string);

                    tracks.push(TrackMetadata {
                        title,
                        artist,
                        album: None,
                        album_art_url,
                        track_id,
                        source: AudioSource::YouTubeMusic,
                    });
                }
            }
        }

        // If specific tab structure didn't yield, fallback to general item search
        if tracks.is_empty() {
            if let Some(items) = json["contents"]["sectionListRenderer"]["contents"].as_array() {
                for item in items {
                    if let Some(video) = item["musicResponsiveListItemRenderer"].as_object() {
                        let val = Value::Object(video.clone());
                        let track_id = val["playlistItemData"]["videoId"]
                            .as_str()
                            .unwrap_or_default()
                            .to_string();
                        if !track_id.is_empty() {
                            let title = val["flexColumns"][0]
                                ["musicResponsiveListItemFlexColumnRenderer"]["text"]["runs"][0]["text"]
                                .as_str()
                                .unwrap_or("Unknown Title")
                                .to_string();
                            let artist = val["flexColumns"][1]
                                ["musicResponsiveListItemFlexColumnRenderer"]["text"]["runs"][0]["text"]
                                .as_str()
                                .unwrap_or("Unknown Artist")
                                .to_string();
                            tracks.push(TrackMetadata {
                                title,
                                artist,
                                album: None,
                                album_art_url: None,
                                track_id,
                                source: AudioSource::YouTubeMusic,
                            });
                        }
                    }
                }
            }
        }

        Ok(tracks)
    }

    pub async fn extract_video_info(&self, video_id: &str) -> Result<(TrackMetadata, String), EngineError> {
        let body = serde_json::json!({
            "videoId": video_id,
            "context": Self::build_android_context(),
            "playbackContext": {
                "contentPlaybackContext": {
                    "html5Preference": "HTML5_PREF_WANTS"
                }
            }
        });

        let resp = self
            .client
            .post("https://www.youtube.com/youtubei/v1/player")
            .header("Content-Type", "application/json")
            .header("X-YouTube-Client-Name", "3")
            .header("X-YouTube-Client-Version", "19.29.35")
            .json(&body)
            .send()
            .await
            .map_err(|e| EngineError::Network(format!("Player API request failed: {e}")))?;

        let json: Value = resp
            .json()
            .await
            .map_err(|e| EngineError::Extractor(format!("Failed to parse player response: {e}")))?;

        let title = json["videoDetails"]["title"]
            .as_str()
            .unwrap_or("Unknown Title")
            .to_string();
        let artist = json["videoDetails"]["author"]
            .as_str()
            .unwrap_or("Unknown Artist")
            .to_string();
        let album_art_url = json["videoDetails"]["thumbnail"]["thumbnails"]
            .as_array()
            .and_then(|arr| arr.last())
            .and_then(|t| t["url"].as_str())
            .map(str::to_string);

        let metadata = TrackMetadata {
            title,
            artist,
            album: None,
            album_art_url,
            track_id: video_id.to_string(),
            source: AudioSource::YouTubeMusic,
        };

        // Find best audio format URL
        let adaptive_formats = json["streamingData"]["adaptiveFormats"]
            .as_array()
            .ok_or_else(|| EngineError::Extractor("No adaptiveFormats found in streamingData".to_string()))?;

        let stream_url = adaptive_formats
            .iter()
            .filter(|f| {
                f["mimeType"]
                    .as_str()
                    .map(|m| m.starts_with("audio/"))
                    .unwrap_or(false)
            })
            .filter_map(|f| f["url"].as_str())
            .next()
            .or_else(|| {
                // If direct adaptive format url missing, check formats
                json["streamingData"]["formats"]
                    .as_array()?
                    .iter()
                    .filter_map(|f| f["url"].as_str())
                    .next()
            })
            .ok_or_else(|| EngineError::Extractor("No direct audio stream URL available".to_string()))?
            .to_string();

        Ok((metadata, stream_url))
    }
}
