pub mod data;
pub mod domain;
pub mod download;
pub mod ffi;
pub mod media;
pub mod network;

uniffi::setup_scaffolding!();

pub mod scraper;

#[uniffi::export]
pub fn hello_world() -> String {
    "Hello from M-Scraper Core!".to_string()
}
