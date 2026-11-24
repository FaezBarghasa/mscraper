use mm_dlp_core::download::manager::DownloadManager;
use mm_dlp_core::ffi::api::{AudioFormat, AudioQuality, AudioSource, MmDlpApi, Playlist, Track};
use mm_dlp_core::network::quic_client::QuicClient;

#[test]
fn test_download_manager_calculate_missing_ranges() {
    let rt = tokio::runtime::Runtime::new().unwrap();
    rt.block_on(async {
        let temp_dir = std::env::temp_dir().join("test_download_manager");
        let _ = std::fs::create_dir_all(&temp_dir);
        let manager = DownloadManager::new(&temp_dir);
        let ranges = manager.calculate_missing_ranges("test_file_ranges", 1000, 4).await.unwrap();
        assert_eq!(ranges.len(), 4);
        assert_eq!(ranges[0], (0, 249));
        assert_eq!(ranges[1], (250, 499));
        assert_eq!(ranges[2], (500, 749));
        assert_eq!(ranges[3], (750, 999));
        let _ = std::fs::remove_dir_all(&temp_dir);
    });
}

#[test]
fn test_mm_dlp_api_search_and_playlist() {
    let api = MmDlpApi::new();
    let results = api.search("Cyberpunk".to_string(), AudioSource::YouTubeMusic).unwrap();
    assert_eq!(results.len(), 2);
    assert_eq!(results[0].source, AudioSource::YouTubeMusic);

    let playlist = Playlist {
        id: "pl_1".to_string(),
        name: "Neon Nights".to_string(),
        description: Some("Cyber aesthetic playlist".to_string()),
        tracks: vec![Track {
            id: "tr_1".to_string(),
            title: "Resonance".to_string(),
            artist: "HOME".to_string(),
            album: Some("Odyssey".to_string()),
            source_url: "https://example.com/res".to_string(),
            duration: 212,
        }],
        source: AudioSource::YouTubeMusic,
    };

    let json = api.export_playlist_json(playlist.clone()).unwrap();
    assert!(json.contains("Neon Nights"));
    assert!(json.contains("Resonance"));

    let imported = api.import_playlist_json(json).unwrap();
    assert_eq!(imported.name, "Neon Nights");
    assert_eq!(imported.tracks.len(), 1);
    assert_eq!(imported.tracks[0].title, "Resonance");

    let xml = api.export_playlist_xml(playlist).unwrap();
    assert!(xml.contains("<playlist>"));
    assert!(xml.contains("Resonance"));
}

#[tokio::test]
async fn test_quic_client_init_webpki_roots() {
    let client = QuicClient::new();
    assert!(client.is_ok(), "QuicClient::new failed with: {:?}", client.err());
}

#[test]
fn test_download_track_audio_container() {
    let api = MmDlpApi::new();
    let temp_dir = std::env::temp_dir();
    let file_path = api.download_track(
        "https://example.com/audio.mp3".to_string(),
        AudioQuality::High,
        AudioFormat::Mp3,
        temp_dir.to_string_lossy().to_string(),
    ).unwrap();
    let path = std::path::Path::new(&file_path);
    assert!(path.exists());
    let metadata = std::fs::metadata(path).unwrap();
    assert!(metadata.len() > 0);
    let _ = std::fs::remove_file(path);
}
