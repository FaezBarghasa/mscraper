use crate::network::quic_client::QuicClient;
use std::path::PathBuf;
use std::sync::RwLock;

#[derive(uniffi::Object)]
pub struct MmDlpEngine {
    quic_enabled: RwLock<bool>,
}

#[uniffi::export]
impl MmDlpEngine {
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

    pub async fn download_track(
        &self,
        url: String,
        _quality: String,
        _format: String,
        temp_dir: String,
    ) -> Result<String, String> {
        let enable_quic = *self.quic_enabled.read().unwrap_or_else(|e| e.into_inner());
        let quic_client = QuicClient::new().map_err(|e| e.to_string())?;

        let bytes = quic_client
            .fetch(&url, enable_quic)
            .await
            .map_err(|e| e.to_string())?;

        let file_path = PathBuf::from(temp_dir).join("downloaded_track.ext");

        tokio::fs::write(&file_path, bytes)
            .await
            .map_err(|e| e.to_string())?;

        Ok(file_path.to_string_lossy().to_string())
    }
}
