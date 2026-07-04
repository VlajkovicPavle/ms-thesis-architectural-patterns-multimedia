#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUTPUT="${BENCHMARK_VIDEO_PATH:-$ROOT_DIR/benchmark/data/videos/smoke-720p-10s.mp4}"
DURATION="${BENCHMARK_VIDEO_DURATION:-10}"
SIZE="${BENCHMARK_VIDEO_SIZE:-1280x720}"
FPS="${BENCHMARK_VIDEO_FPS:-30}"
ENCODERS="$(ffmpeg -hide_banner -encoders 2>/dev/null)"
VIDEO_ARGS=()

if [[ "$ENCODERS" == *"libx264"* ]]; then
  VIDEO_ARGS=(-c:v libx264 -preset veryfast -crf 23)
elif [[ "$ENCODERS" == *"libopenh264"* ]]; then
  VIDEO_ARGS=(-c:v libopenh264 -b:v 2500k)
else
  printf 'No supported H.264 encoder found. Expected libx264 or libopenh264.\n' >&2
  exit 1
fi

mkdir -p "$(dirname "$OUTPUT")"

ffmpeg \
  -hide_banner \
  -loglevel error \
  -f lavfi \
  -i "testsrc2=size=$SIZE:rate=$FPS:duration=$DURATION" \
  -f lavfi \
  -i "sine=frequency=1000:sample_rate=48000:duration=$DURATION" \
  -map 0:v:0 \
  -map 1:a:0 \
  "${VIDEO_ARGS[@]}" \
  -pix_fmt yuv420p \
  -c:a aac \
  -b:a 128k \
  -movflags +faststart \
  -y \
  "$OUTPUT"

printf 'Generated deterministic video: %s\n' "$OUTPUT"
