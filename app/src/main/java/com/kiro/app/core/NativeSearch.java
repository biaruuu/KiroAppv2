package com.kiro.app.core;

/**
 * Bridge to the Rust search ranker (native/ → libkiro_core.so).
 * If the library was not built/packaged, {@link #rankOrNull} returns null and callers use the Kotlin fallback.
 */
public final class NativeSearch {
    private static final boolean LOADED;

    static {
        boolean ok;
        try {
            System.loadLibrary("kiro_core");
            ok = true;
        } catch (UnsatisfiedLinkError e) {
            ok = false;
        }
        LOADED = ok;
    }

    private NativeSearch() {
    }

    public static boolean isAvailable() {
        return LOADED;
    }

    /**
     * @return indices into {@code texts}, best match first, only items that match; or null if native code is unavailable.
     */
    public static int[] rankOrNull(String query, String[] texts) {
        if (!LOADED) return null;
        try {
            return rank(query, texts);
        } catch (UnsatisfiedLinkError e) {
            return null;
        }
    }

    // Implemented in native/src/lib.rs as Java_com_kiro_app_core_NativeSearch_rank
    private static native int[] rank(String query, String[] texts);
}
