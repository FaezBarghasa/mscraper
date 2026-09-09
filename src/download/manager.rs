use crate::download::DownloadError;
use std::path::{Path, PathBuf};
use tokio::fs;
use tokio::io::AsyncWriteExt;
use tokio::sync::mpsc;

pub struct DownloadTask {
    pub url: String,
    pub file_id: String,
}

pub struct DownloadManager {
    temp_dir: PathBuf,
}

impl DownloadManager {
    pub fn new(temp_dir: impl AsRef<Path>) -> Self {
        Self {
            temp_dir: temp_dir.as_ref().to_path_buf(),
        }
    }

    pub async fn process_queue(&self, mut rx: mpsc::Receiver<DownloadTask>) {
        while let Some(task) = rx.recv().await {
            // Simplified handling for the demo
            println!("Processing download for {}", task.url);
        }
    }

    pub async fn calculate_missing_ranges(
        &self,
        file_id: &str,
        total_size: u64,
        num_chunks: u64,
    ) -> Result<Vec<(u64, u64)>, DownloadError> {
        let mut missing_ranges = Vec::new();
        let chunk_size = total_size / num_chunks;

        for i in 0..num_chunks {
            let start = i * chunk_size;
            let end = if i == num_chunks - 1 {
                total_size - 1
            } else {
                start + chunk_size - 1
            };

            let part_path = self.temp_dir.join(format!("{}.part{}", file_id, i));

            if part_path.exists() {
                let metadata = fs::metadata(&part_path).await?;
                let part_size = metadata.len();

                let expected_size = end - start + 1;

                if part_size < expected_size {
                    missing_ranges.push((start + part_size, end));
                }
            } else {
                missing_ranges.push((start, end));
            }
        }

        Ok(missing_ranges)
    }

    pub async fn merge_chunks(
        &self,
        file_id: &str,
        num_chunks: u64,
        final_path: impl AsRef<Path>,
    ) -> Result<(), DownloadError> {
        let mut final_file = fs::OpenOptions::new()
            .create(true)
            .write(true)
            .truncate(true)
            .open(&final_path)
            .await?;

        for i in 0..num_chunks {
            let part_path = self.temp_dir.join(format!("{}.part{}", file_id, i));
            let mut part_file = fs::File::open(&part_path).await?;

            tokio::io::copy(&mut part_file, &mut final_file).await?;

            fs::remove_file(part_path).await?;
        }

        final_file.flush().await?;

        Ok(())
    }
}
