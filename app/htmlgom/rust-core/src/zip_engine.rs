use std::fs::{self, File};
use std::io::{Read, Write};
use std::os::raw::c_int;
use std::os::unix::io::FromRawFd;
use std::path::{Component, Path, PathBuf};
use zip::write::SimpleFileOptions;
use zip::{CompressionMethod, ZipArchive, ZipWriter};

const BUFFER_SIZE: usize = 65536; // 64 KB streaming buffer
const MAX_TOTAL_UNCOMPRESSED: u64 = 500 * 1024 * 1024; // 500 MB zip bomb limit
const MAX_RATIO: u64 = 100; // 100:1 ratio cap for files > 1MB

#[derive(Debug)]
pub enum ZipError {
    IoError,
    InvalidZip,
    InvalidArgument,
    PathTraversal,
    SymlinkRejected,
    BombDetected,
}

impl From<ZipError> for i32 {
    fn from(err: ZipError) -> Self {
        match err {
            ZipError::IoError => -1,
            ZipError::InvalidZip => -2,
            ZipError::InvalidArgument => -3,
            ZipError::PathTraversal => -4,
            ZipError::SymlinkRejected => -5,
            ZipError::BombDetected => -6,
        }
    }
}

impl From<std::io::Error> for ZipError {
    fn from(_: std::io::Error) -> Self {
        ZipError::IoError
    }
}

impl From<zip::result::ZipError> for ZipError {
    fn from(err: zip::result::ZipError) -> Self {
        match err {
            zip::result::ZipError::Io(e) => ZipError::from(e),
            zip::result::ZipError::InvalidArchive(_) | zip::result::ZipError::UnsupportedArchive(_) => {
                ZipError::InvalidZip
            }
            _ => ZipError::InvalidZip,
        }
    }
}

/// Sanitizes relative output paths against Zip-Slip vulnerabilities.
fn sanitize_entry_path(entry_name: &str) -> Result<PathBuf, ZipError> {
    let clean = entry_name.replace('\\', "/");
    let path = Path::new(&clean);
    let mut safe_path = PathBuf::new();

    for component in path.components() {
        match component {
            Component::Normal(segment) => {
                safe_path.push(segment);
            }
            Component::CurDir => {}
            Component::ParentDir | Component::RootDir | Component::Prefix(_) => {
                return Err(ZipError::PathTraversal);
            }
        }
    }

    if safe_path.as_os_str().is_empty() {
        return Err(ZipError::PathTraversal);
    }

    Ok(safe_path)
}

/// Streams decompression of a ZIP archive from a kernel file descriptor.
/// Duplicates the descriptor via libc::dup to eliminate double-close races with Android ART.
pub fn extract_zip_streaming(fd_in: c_int, output_dir_str: &str) -> Result<i32, ZipError> {
    if fd_in < 0 || output_dir_str.is_empty() {
        return Err(ZipError::InvalidArgument);
    }

    let dup_fd = unsafe { libc::dup(fd_in) };
    if dup_fd < 0 {
        return Err(ZipError::IoError);
    }

    let file = unsafe { File::from_raw_fd(dup_fd) };
    let mut archive = ZipArchive::new(file)?;
    let output_dir = Path::new(output_dir_str);

    if !output_dir.exists() {
        fs::create_dir_all(output_dir)?;
    }

    let mut total_uncompressed: u64 = 0;
    let mut extracted_count = 0;
    let mut buffer = [0u8; BUFFER_SIZE];

    for i in 0..archive.len() {
        let mut entry = archive.by_index(i)?;

        // Reject symbolic links to prevent sandbox escaping
        #[cfg(unix)]
        if let Some(mode) = entry.unix_mode() {
            if (mode & 0o170000) == 0o120000 {
                return Err(ZipError::SymlinkRejected);
            }
        }

        // Validate safe destination path (Zip-Slip defense)
        let relative_path = sanitize_entry_path(entry.name())?;
        let target_path = output_dir.join(&relative_path);

        if entry.is_dir() {
            fs::create_dir_all(&target_path)?;
            continue;
        }

        // Bomb defense checks
        let uncompressed_size = entry.size();
        let compressed_size = entry.compressed_size();

        total_uncompressed = total_uncompressed.saturating_add(uncompressed_size);
        if total_uncompressed > MAX_TOTAL_UNCOMPRESSED {
            return Err(ZipError::BombDetected);
        }

        if uncompressed_size > 1024 * 1024 && compressed_size > 0 {
            if uncompressed_size / compressed_size > MAX_RATIO {
                return Err(ZipError::BombDetected);
            }
        }

        if let Some(parent) = target_path.parent() {
            if !parent.exists() {
                fs::create_dir_all(parent)?;
            }
        }

        let mut out_file = File::create(&target_path)?;
        loop {
            let bytes_read = entry.read(&mut buffer)?;
            if bytes_read == 0 {
                break;
            }
            out_file.write_all(&buffer[..bytes_read])?;
        }

        extracted_count += 1;
    }

    Ok(extracted_count)
}

/// Compresses a set of source file paths into a ZIP output descriptor using Deflate.
pub fn compress_paths_to_zip(
    fd_out: c_int,
    file_paths: &[String],
    entry_names: &[String],
) -> Result<i64, ZipError> {
    if fd_out < 0 || file_paths.len() != entry_names.len() {
        return Err(ZipError::InvalidArgument);
    }

    let dup_fd = unsafe { libc::dup(fd_out) };
    if dup_fd < 0 {
        return Err(ZipError::IoError);
    }

    let file = unsafe { File::from_raw_fd(dup_fd) };
    let mut writer = ZipWriter::new(file);
    let options = SimpleFileOptions::default().compression_method(CompressionMethod::Deflated);

    let mut buffer = [0u8; BUFFER_SIZE];
    let mut total_written: i64 = 0;

    for (src_path, entry_name) in file_paths.iter().zip(entry_names.iter()) {
        let safe_entry = sanitize_entry_path(entry_name)?;
        let entry_str = safe_entry.to_string_lossy();

        let mut src_file = File::open(src_path)?;
        writer.start_file(entry_str, options)?;

        loop {
            let bytes_read = src_file.read(&mut buffer)?;
            if bytes_read == 0 {
                break;
            }
            writer.write_all(&buffer[..bytes_read])?;
            total_written += bytes_read as i64;
        }
    }

    writer.finish()?;
    Ok(total_written)
}
