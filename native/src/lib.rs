//! Kiro native core: JNI entry points for the Android app.

mod ranker;

use jni::objects::{JClass, JObjectArray, JString};
use jni::sys::jintArray;
use jni::JNIEnv;

/// `int[] NativeSearch.rank(String query, String[] texts)`
///
/// Returns indices of matching `texts`, best match first. Returns null on any JNI error
/// (the Kotlin side then falls back to its own ranker).
#[no_mangle]
pub extern "system" fn Java_com_kiro_app_core_NativeSearch_rank<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    query: JString<'local>,
    texts: JObjectArray<'local>,
) -> jintArray {
    match rank_impl(&mut env, &query, &texts) {
        Ok(arr) => arr,
        Err(_) => {
            // Clear any pending Java exception so the caller can continue with the fallback.
            let _ = env.exception_clear();
            std::ptr::null_mut()
        }
    }
}

fn rank_impl(
    env: &mut JNIEnv,
    query: &JString,
    texts: &JObjectArray,
) -> jni::errors::Result<jintArray> {
    let q: String = env.get_string(query)?.into();

    let len = env.get_array_length(texts)?;
    let mut items: Vec<String> = Vec::with_capacity(len as usize);
    for i in 0..len {
        let obj = env.get_object_array_element(texts, i)?;
        let js = JString::from(obj);
        let s: String = env.get_string(&js)?.into();
        env.delete_local_ref(js)?;
        items.push(s);
    }

    let order = ranker::rank_indices(&q, &items);

    let out = env.new_int_array(order.len() as i32)?;
    env.set_int_array_region(&out, 0, &order)?;
    Ok(out.into_raw())
}
