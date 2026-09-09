pub mod soundcloud;
pub mod youtube;

use crate::domain::models::StreamInfo;
use async_trait::async_trait;

#[async_trait]
pub trait Extractor {
    async fn extract(&self, url: &str) -> Result<StreamInfo, crate::media::MediaError>;
}
