use base64::Engine;
use std::fs::File;
use std::io::Write;
use std::os::raw::c_int;
use std::path::{Component, Path, PathBuf};

#[derive(Debug)]
pub enum ArchiveError {
    MmapFailed,
    InvalidMhtml,
    IoError,
    PathTraversal,
}

impl From<ArchiveError> for i32 {
    fn from(err: ArchiveError) -> Self {
        match err {
            ArchiveError::MmapFailed => -1,
            ArchiveError::InvalidMhtml => -2,
            ArchiveError::IoError => -3,
            ArchiveError::PathTraversal => -4,
        }
    }
}

/// Sanitizes relative output paths against Zip-Slip / directory traversal vulnerabilities.
fn sanitize_output_path(base_dir: &Path, raw_path_str: &str) -> Result<PathBuf, ArchiveError> {
    let raw_clean = raw_path_str
        .trim()
        .trim_matches('"')
        .trim_matches('\'')
        .trim_start_matches("http://")
        .trim_start_matches("https://");

    // Extract filename portion or safe relative path
    let relative_path = Path::new(raw_clean);
    let mut safe_path = PathBuf::from(base_dir);

    for component in relative_path.components() {
        match component {
            Component::Normal(segment) => {
                safe_path.push(segment);
            }
            Component::CurDir => {}
            Component::ParentDir | Component::RootDir | Component::Prefix(_) => {
                // Reject traversal attempts
                return Err(ArchiveError::PathTraversal);
            }
        }
    }

    Ok(safe_path)
}

/// Extracts multipart sections from an MHTML document.
pub fn parse_mhtml_streaming(
    fd_in: c_int,
    length: usize,
    output_dir_str: &str,
) -> Result<i32, ArchiveError> {
    if length == 0 {
        return Ok(0);
    }

    let mmap_ptr = unsafe {
        libc::mmap(
            std::ptr::null_mut(),
            length,
            libc::PROT_READ,
            libc::MAP_SHARED,
            fd_in,
            0,
        )
    };

    if mmap_ptr == libc::MAP_FAILED {
        return Err(ArchiveError::MmapFailed);
    }

    let data = unsafe { std::slice::from_raw_parts(mmap_ptr as *const u8, length) };

    let output_dir = Path::new(output_dir_str);
    if !output_dir.exists() {
        std::fs::create_dir_all(output_dir).map_err(|_| ArchiveError::IoError)?;
    }

    let result = extract_mhtml_parts(data, output_dir);

    unsafe {
        libc::munmap(mmap_ptr, length);
    }

    result
}

fn extract_mhtml_parts(data: &[u8], output_dir: &Path) -> Result<i32, ArchiveError> {
    // 1. Locate boundary marker in headers
    let boundary_key = b"boundary=";
    let boundary_pos = memchr::memmem::find(data, boundary_key).ok_or(ArchiveError::InvalidMhtml)?;
    let boundary_start = boundary_pos + boundary_key.len();

    let mut boundary_end = boundary_start;
    while boundary_end < data.len() && data[boundary_end] != b'\r' && data[boundary_end] != b'\n' && data[boundary_end] != b';' {
        boundary_end += 1;
    }

    let raw_boundary = &data[boundary_start..boundary_end];
    let boundary_str = std::str::from_utf8(raw_boundary).map_err(|_| ArchiveError::InvalidMhtml)?
        .trim()
        .trim_matches('"')
        .trim_matches('\'');

    if boundary_str.is_empty() {
        return Err(ArchiveError::InvalidMhtml);
    }

    let delimiter = format!("--{}", boundary_str);
    let delimiter_bytes = delimiter.as_bytes();

    let mut parts_extracted = 0;
    let mut search_cursor = 0;

    // Scan for parts
    while let Some(part_start_offset) = memchr::memmem::find(&data[search_cursor..], delimiter_bytes) {
        let abs_part_start = search_cursor + part_start_offset + delimiter_bytes.len();
        // Check for closing boundary '--'
        if abs_part_start + 2 <= data.len() && &data[abs_part_start..abs_part_start + 2] == b"--" {
            break;
        }

        search_cursor = abs_part_start;

        // Find the next boundary marking the end of this part
        let next_boundary = match memchr::memmem::find(&data[search_cursor..], delimiter_bytes) {
            Some(offset) => search_cursor + offset,
            None => data.len(),
        };

        let part_data = &data[search_cursor..next_boundary];
        if let Ok(true) = process_single_part(part_data, output_dir, parts_extracted) {
            parts_extracted += 1;
        }

        search_cursor = next_boundary;
    }

    Ok(parts_extracted)
}

fn process_single_part(part: &[u8], output_dir: &Path, part_index: i32) -> Result<bool, ArchiveError> {
    // Separate headers from body (delimited by \r\n\r\n or \n\n)
    let header_delimiter = if let Some(pos) = memchr::memmem::find(part, b"\r\n\r\n") {
        Some((pos, 4))
    } else {
        memchr::memmem::find(part, b"\n\n").map(|pos| (pos, 2))
    };

    let (headers_len, delim_len) = match header_delimiter {
        Some(val) => val,
        None => return Ok(false),
    };

    let headers_bytes = &part[..headers_len];
    let body_bytes = &part[headers_len + delim_len..];

    let headers_str = String::from_utf8_lossy(headers_bytes);
    let mut location: Option<String> = None;
    let mut is_base64 = false;

    for line in headers_str.lines() {
        let line_trimmed = line.trim();
        if line_trimmed.to_ascii_lowercase().starts_with("content-location:") {
            let val = line_trimmed[17..].trim();
            location = Some(val.to_string());
        } else if line_trimmed.to_ascii_lowercase().starts_with("content-transfer-encoding:") {
            let val = line_trimmed[26..].trim().to_ascii_lowercase();
            if val.contains("base64") {
                is_base64 = true;
            }
        }
    }

    let filename = match location {
        Some(loc) => {
            let clean = loc.split('?').next().unwrap_or(&loc);
            clean.rsplit('/').next().unwrap_or(&loc).to_string()
        }
        None => {
            if part_index == 0 {
                "index.html".to_string()
            } else {
                format!("part_{}.bin", part_index)
            }
        }
    };

    let target_path = sanitize_output_path(output_dir, &filename)?;
    let mut out_file = File::create(&target_path).map_err(|_| ArchiveError::IoError)?;

    if is_base64 {
        // Strip newlines/whitespace from base64 body before decoding
        let clean_base64: Vec<u8> = body_bytes
            .iter()
            .copied()
            .filter(|&b| b != b'\r' && b != b'\n' && b != b' ' && b != b'\t')
            .collect();

        if let Ok(decoded) = base64::engine::general_purpose::STANDARD.decode(&clean_base64) {
            out_file.write_all(&decoded).map_err(|_| ArchiveError::IoError)?;
        } else {
            // Write raw if decoding fails
            out_file.write_all(body_bytes).map_err(|_| ArchiveError::IoError)?;
        }
    } else {
        out_file.write_all(body_bytes).map_err(|_| ArchiveError::IoError)?;
    }

    Ok(true)
}
