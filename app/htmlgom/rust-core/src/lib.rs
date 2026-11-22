pub mod archive;
pub mod parser;
pub mod zip_engine;

use jni_sys::{jclass, jint, jlong, jobjectArray, jsize, jstring, JNIEnv};
use std::ffi::CStr;
use std::panic::catch_unwind;

unsafe fn jstring_to_string(env: *mut JNIEnv, jstr: jstring) -> Option<String> {
    if jstr.is_null() {
        return None;
    }
    let get_fn = (**env).GetStringUTFChars?;
    let release_fn = (**env).ReleaseStringUTFChars?;

    let chars = get_fn(env, jstr, std::ptr::null_mut());
    if chars.is_null() {
        return None;
    }

    let cstr = CStr::from_ptr(chars);
    let result = cstr.to_string_lossy().into_owned();
    release_fn(env, jstr, chars);
    Some(result)
}

unsafe fn jobject_array_to_strings(env: *mut JNIEnv, array: jobjectArray) -> Option<Vec<String>> {
    if array.is_null() {
        return None;
    }
    let get_len_fn = (**env).GetArrayLength?;
    let get_elem_fn = (**env).GetObjectArrayElement?;
    let del_local_ref_fn = (**env).DeleteLocalRef?;

    let len = get_len_fn(env, array) as usize;
    let mut vec = Vec::with_capacity(len);

    for i in 0..len {
        let elem = get_elem_fn(env, array, i as jsize);
        if !elem.is_null() {
            if let Some(s) = jstring_to_string(env, elem as jstring) {
                vec.push(s);
            }
            del_local_ref_fn(env, elem);
        }
    }
    Some(vec)
}

#[no_mangle]
pub unsafe extern "system" fn Java_com_absolutemango_htmlgom_core_NativeThermalCore_nativeSanitizeHtml(
    env: *mut JNIEnv,
    _class: jclass,
    fd_in: jint,
    length: jlong,
    cache_prefix_jstr: jstring,
    fd_out: jint,
) -> jint {
    let result = catch_unwind(std::panic::AssertUnwindSafe(|| {
        if length <= 0 || fd_in < 0 || fd_out < 0 {
            return -3; // Invalid argument
        }

        let cache_prefix = match jstring_to_string(env, cache_prefix_jstr) {
            Some(s) => s,
            None => return -3,
        };

        match parser::sanitize_and_rewrite_html(
            fd_in as std::os::raw::c_int,
            length as usize,
            cache_prefix.as_bytes(),
            fd_out as std::os::raw::c_int,
        ) {
            Ok(written) => written as jint,
            Err(e) => i32::from(e),
        }
    }));

    match result {
        Ok(code) => code,
        Err(_) => -100, // Panic caught safely
    }
}

#[no_mangle]
pub unsafe extern "system" fn Java_com_absolutemango_htmlgom_core_NativeThermalCore_nativeParseMhtml(
    env: *mut JNIEnv,
    _class: jclass,
    fd_in: jint,
    length: jlong,
    output_dir_jstr: jstring,
) -> jint {
    let result = catch_unwind(std::panic::AssertUnwindSafe(|| {
        if length <= 0 || fd_in < 0 {
            return -3; // Invalid argument
        }

        let output_dir = match jstring_to_string(env, output_dir_jstr) {
            Some(s) => s,
            None => return -3,
        };

        match archive::parse_mhtml_streaming(
            fd_in as std::os::raw::c_int,
            length as usize,
            &output_dir,
        ) {
            Ok(count) => count,
            Err(e) => i32::from(e),
        }
    }));

    match result {
        Ok(code) => code,
        Err(_) => -100, // Panic caught safely
    }
}

#[no_mangle]
pub unsafe extern "system" fn Java_com_absolutemango_htmlgom_core_NativeThermalCore_nativeExtractZip(
    env: *mut JNIEnv,
    _class: jclass,
    fd_in: jint,
    _length: jlong,
    output_dir_jstr: jstring,
) -> jint {
    let result = catch_unwind(std::panic::AssertUnwindSafe(|| {
        if fd_in < 0 {
            return -3; // Invalid argument
        }

        let output_dir = match jstring_to_string(env, output_dir_jstr) {
            Some(s) => s,
            None => return -3,
        };

        match zip_engine::extract_zip_streaming(fd_in as std::os::raw::c_int, &output_dir) {
            Ok(count) => count,
            Err(e) => i32::from(e),
        }
    }));

    match result {
        Ok(code) => code,
        Err(_) => -100, // Panic caught safely
    }
}

#[no_mangle]
pub unsafe extern "system" fn Java_com_absolutemango_htmlgom_core_NativeThermalCore_nativeCompressZip(
    env: *mut JNIEnv,
    _class: jclass,
    fd_out: jint,
    src_paths_jarray: jobjectArray,
    entry_names_jarray: jobjectArray,
) -> jlong {
    let result = catch_unwind(std::panic::AssertUnwindSafe(|| {
        if fd_out < 0 {
            return -3; // Invalid argument
        }

        let src_paths = match jobject_array_to_strings(env, src_paths_jarray) {
            Some(v) => v,
            None => return -3,
        };

        let entry_names = match jobject_array_to_strings(env, entry_names_jarray) {
            Some(v) => v,
            None => return -3,
        };

        if src_paths.len() != entry_names.len() {
            return -3;
        }

        match zip_engine::compress_paths_to_zip(
            fd_out as std::os::raw::c_int,
            &src_paths,
            &entry_names,
        ) {
            Ok(bytes) => bytes,
            Err(e) => i32::from(e) as jlong,
        }
    }));

    match result {
        Ok(code) => code,
        Err(_) => -100, // Panic caught safely
    }
}
