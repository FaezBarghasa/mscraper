use crate::domain::models::TrackMetadata;
use reqwest::Client;
use serde::Deserialize;

#[derive(Deserialize)]
struct MusicBrainzReleaseGroup {
    title: String,
}

#[derive(Deserialize)]
struct MusicBrainzRelease {
    title: String,
    #[serde(rename = "release-group")]
    release_group: Option<MusicBrainzReleaseGroup>,
}

#[derive(Deserialize)]
struct MusicBrainzRecording {
    id: String,
    title: String,
    releases: Option<Vec<MusicBrainzRelease>>,
}

#[derive(Deserialize)]
struct MusicBrainzResponse {
    recordings: Vec<MusicBrainzRecording>,
}

pub async fn rescue_metadata(title: &str, artist: &str) -> Option<TrackMetadata> {
    let client = Client::new();
    let query = format!("recording:\"{}\" AND artist:\"{}\"", title, artist);
    let url = format!(
        "https://musicbrainz.org/ws/2/recording/?query={}&fmt=json",
        urlencoding::encode(&query)
    );

    let request = client
        .get(&url)
        .header("User-Agent", "mscraper/0.1.0 ( your-email@example.com )")
        .timeout(std::time::Duration::from_secs(10));

    match request.send().await {
        Ok(resp) if resp.status().is_success() => {
            if let Ok(data) = resp.json::<MusicBrainzResponse>().await {
                if let Some(recording) = data.recordings.first() {
                    let mut album = None;
                    if let Some(releases) = &recording.releases {
                        if let Some(release) = releases.first() {
                            if let Some(rg) = &release.release_group {
                                album = Some(rg.title.clone());
                            } else {
                                album = Some(release.title.clone());
                            }
                        }
                    }

                    let album_art_url = if album.is_some() {
                        Some(format!(
                            "https://coverartarchive.org/release-group/{}/front-500",
                            recording.id
                        ))
                    } else {
                        None
                    };

                    return Some(TrackMetadata {
                        id: recording.id.clone(),
                        title: recording.title.clone(),
                        artist: artist.to_string(),
                        album,
                        album_art_url,
                        duration_ms: 0,
                    });
                }
            }
        }
        _ => {}
    }
    None
}
