#!/usr/bin/env bash
# Builds the Rust search engine (native/) into app/src/main/jniLibs for the Android app.
# Requirements: rustup, Android NDK (set ANDROID_NDK_HOME), cargo-ndk.
set -euo pipefail

cd "$(dirname "$0")/native"

rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
command -v cargo-ndk >/dev/null 2>&1 || cargo install cargo-ndk --locked

cargo test
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -o ../app/src/main/jniLibs build --release

echo "Done: app/src/main/jniLibs now contains libkiro_core.so"
