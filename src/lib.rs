#![allow(clippy::double_must_use)]

pub mod data;
pub mod domain;
pub mod download;
pub mod ffi;
pub mod media;
pub mod network;
pub mod scraper;

uniffi::setup_scaffolding!();
