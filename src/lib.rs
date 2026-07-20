uniffi::setup_scaffolding!();

pub mod scraper;

#[uniffi::export]
pub fn hello_world() -> String {
    "Hello from M-Scraper Core!".to_string()
}
