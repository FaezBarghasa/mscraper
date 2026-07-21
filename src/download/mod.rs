pub mod manager;

#[derive(thiserror::Error, Debug)]
pub enum DownloadError {
    #[error("File Error: {0}")]
    FileError(#[from] std::io::Error),
    #[error("Network Error: {0}")]
    NetworkError(String),
}
