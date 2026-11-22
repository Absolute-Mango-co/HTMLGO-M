use std::os::raw::c_int;

#[derive(Debug)]
pub enum ParserError {
    MmapFailed,
    IoError,
    InvalidInput,
}

impl From<ParserError> for i32 {
    fn from(err: ParserError) -> Self {
        match err {
            ParserError::MmapFailed => -1,
            ParserError::IoError => -2,
            ParserError::InvalidInput => -3,
        }
    }
}

/// Checks if a URL string is relative and eligible for local cache redirection.
#[inline]
fn is_relative_url(url: &[u8]) -> bool {
    if url.is_empty() {
        return false;
    }
    // Skip protocol-relative, anchors, javascript, data, or absolute protocols
    let starts_with = |prefix: &[u8]| -> bool {
        url.len() >= prefix.len() && url[..prefix.len()].eq_ignore_ascii_case(prefix)
    };

    if url.starts_with(b"#")
        || url.starts_with(b"/")
        || starts_with(b"http://")
        || starts_with(b"https://")
        || starts_with(b"data:")
        || starts_with(b"file:")
        || starts_with(b"javascript:")
        || starts_with(b"mailto:")
    {
        return false;
    }

    true
}

/// Segmented borrow-and-stream HTML sanitizer and link rewriter.
/// Maps fd_in read-only, scans for relative resource links using NEON SIMD vector matching,
/// and streams the rewritten content to fd_out without memory shifting.
pub fn sanitize_and_rewrite_html(
    fd_in: c_int,
    length: usize,
    cache_prefix: &[u8],
    fd_out: c_int,
) -> Result<usize, ParserError> {
    if length == 0 {
        return Ok(0);
    }

    // Safety: mmap read-only memory from kernel page cache
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
        return Err(ParserError::MmapFailed);
    }

    let input_slice = unsafe { std::slice::from_raw_parts(mmap_ptr as *const u8, length) };

    let result = process_html_stream(input_slice, cache_prefix, fd_out);

    // Always unmap kernel memory
    unsafe {
        libc::munmap(mmap_ptr, length);
    }

    result
}

fn write_all(fd: c_int, buf: &[u8]) -> Result<(), ParserError> {
    let mut total_written = 0;
    while total_written < buf.len() {
        let chunk = &buf[total_written..];
        let written = unsafe { libc::write(fd, chunk.as_ptr() as *const libc::c_void, chunk.len()) };
        if written <= 0 {
            return Err(ParserError::IoError);
        }
        total_written += written as usize;
    }
    Ok(())
}

fn process_html_stream(
    input: &[u8],
    cache_prefix: &[u8],
    fd_out: c_int,
) -> Result<usize, ParserError> {
    let mut cursor = 0;
    let mut bytes_emitted = 0;

    // Attributes to scan with vector acceleration
    let patterns: [&[u8]; 4] = [b"href=\"", b"href='", b"src=\"", b"src='"];

    while cursor < input.len() {
        let remaining = &input[cursor..];
        let mut earliest_match: Option<(usize, usize, u8)> = None; // (offset, pattern_len, quote_char)

        for &pattern in &patterns {
            if let Some(pos) = memchr::memmem::find(remaining, pattern) {
                let quote = pattern[pattern.len() - 1];
                match earliest_match {
                    Some((earliest_pos, _, _)) if pos < earliest_pos => {
                        earliest_match = Some((pos, pattern.len(), quote));
                    }
                    None => {
                        earliest_match = Some((pos, pattern.len(), quote));
                    }
                    _ => {}
                }
            }
        }

        match earliest_match {
            Some((match_offset, pattern_len, quote_char)) => {
                let url_start = cursor + match_offset + pattern_len;
                // Emit bytes up to the start of the URL
                let prefix_bytes = &input[cursor..url_start];
                write_all(fd_out, prefix_bytes)?;
                bytes_emitted += prefix_bytes.len();

                // Find closing quote
                let url_slice_rest = &input[url_start..];
                if let Some(closing_pos) = memchr::memchr(quote_char, url_slice_rest) {
                    let url = &url_slice_rest[..closing_pos];
                    if is_relative_url(url) {
                        // Emit cache prefix before the relative path
                        write_all(fd_out, cache_prefix)?;
                        bytes_emitted += cache_prefix.len();
                    }
                    // Emit the original url
                    write_all(fd_out, url)?;
                    bytes_emitted += url.len();

                    cursor = url_start + closing_pos;
                } else {
                    cursor = url_start;
                }
            }
            None => {
                // Emit all remaining bytes
                let tail = &input[cursor..];
                write_all(fd_out, tail)?;
                bytes_emitted += tail.len();
                break;
            }
        }
    }

    Ok(bytes_emitted)
}
