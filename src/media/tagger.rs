use crate::domain::models::TrackMetadata;
use crate::media::MediaError;
use lofty::config::WriteOptions;
use lofty::file::TaggedFileExt;
use lofty::picture::{Picture, PictureType};
use lofty::probe::Probe;
use lofty::tag::{Accessor, Tag, TagExt};
use std::path::Path;

pub async fn tag_audio_file(
    file_path: &Path,
    metadata: &TrackMetadata,
    cover_art_data: Option<&[u8]>,
) -> Result<(), MediaError> {
    let mut tagged_file = Probe::open(file_path)
        .map_err(|e| MediaError::TaggingError(e.to_string()))?
        .read()
        .map_err(|e| MediaError::TaggingError(e.to_string()))?;

    let tag_type = tagged_file.primary_tag_type();

    let mut tag = match tagged_file.primary_tag_mut() {
        Some(t) => t.clone(),
        None => {
            if let Some(existing) = tagged_file.first_tag() {
                existing.clone()
            } else {
                Tag::new(tag_type)
            }
        }
    };

    tag.set_title(metadata.title.clone());
    tag.set_artist(metadata.artist.clone());

    if let Some(album) = &metadata.album {
        tag.set_album(album.clone());
    }

    if let Some(cover_art) = cover_art_data {
        let picture = Picture::from_reader(&mut std::io::Cursor::new(cover_art))
            .map_err(|e| MediaError::TaggingError(e.to_string()))?;
        let mut picture = picture;
        picture.set_pic_type(PictureType::CoverFront);
        tag.push_picture(picture);
    }

    tag.save_to_path(file_path, WriteOptions::default())
        .map_err(|e| MediaError::TaggingError(e.to_string()))?;

    Ok(())
}
