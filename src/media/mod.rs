pub mod metadata_rescue;
pub mod tagger;

#[derive(thiserror::Error, Debug)]
pub enum MediaError {
    #[error("Extraction failed: {0}")]
    ExtractionError(String),
    #[error("Network error: {0}")]
    NetworkError(#[from] reqwest::Error),
    #[error("Metadata rescue failed: {0}")]
    RescueError(String),
    #[error("Tagging failed: {0}")]
    TaggingError(String),
    #[error("IO error: {0}")]
    IoError(#[from] std::io::Error),
}
