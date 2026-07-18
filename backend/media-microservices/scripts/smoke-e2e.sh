#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
BASE_URL="${SMOKE_BASE_URL:-http://localhost:8080}"
VIDEO_PATH="${SMOKE_VIDEO_PATH:-$ROOT_DIR/benchmark/data/videos/source-1280x800-10s.mp4}"
POLL_ATTEMPTS="${SMOKE_POLL_ATTEMPTS:-120}"
POLL_PAUSE_SECONDS="${SMOKE_POLL_PAUSE_SECONDS:-1}"
ARTIFACT_DIR="${SMOKE_ARTIFACT_DIR:-$(mktemp -d /tmp/media-microservices-smoke.XXXXXX)}"

for command in curl jq ffprobe; do
  command -v "$command" >/dev/null 2>&1 || {
    printf 'Required command is unavailable: %s\n' "$command" >&2
    exit 1
  }
done

if [ ! -f "$VIDEO_PATH" ]; then
  printf 'Smoke fixture is missing: %s\n' "$VIDEO_PATH" >&2
  exit 1
fi

mkdir -p "$ARTIFACT_DIR"
curl -fsS "$BASE_URL/api/actuator/health" | jq -e '.status == "UP"' >/dev/null

timeout 180 curl -NfsS "$BASE_URL/api/v1/notification/stream" > "$ARTIFACT_DIR/sse.txt" &
SSE_PID=$!
trap 'kill "$SSE_PID" 2>/dev/null || true' EXIT
sleep 1

UPLOAD_RESPONSE="$(
  curl -fsS \
    -H 'Accept: application/json' \
    -F "file=@$VIDEO_PATH;type=video/mp4" \
    "$BASE_URL/api/v1/video"
)"
printf '%s\n' "$UPLOAD_RESPONSE" > "$ARTIFACT_DIR/upload.json"
VIDEO_ID="$(jq -er '.id' <<< "$UPLOAD_RESPONSE")"

curl -fsS \
  -H 'Content-Type: application/json' \
  -d "{\"videoId\":\"$VIDEO_ID\",\"resolutions\":[\"SD_360\",\"HD_720\"]}" \
  "$BASE_URL/api/v1/rendition" \
  -o /dev/null

RENDITIONS='[]'
for ((attempt = 1; attempt <= POLL_ATTEMPTS; attempt++)); do
  RENDITIONS="$(curl -fsS "$BASE_URL/api/v1/rendition/video/$VIDEO_ID")"
  if jq -e 'length == 2 and all(.status == "FINISHED" or .status == "ERROR")' \
    <<< "$RENDITIONS" >/dev/null; then
    break
  fi
  sleep "$POLL_PAUSE_SECONDS"
done
printf '%s\n' "$RENDITIONS" > "$ARTIFACT_DIR/renditions.json"
jq -e 'length == 2 and all(.status == "FINISHED")' <<< "$RENDITIONS" >/dev/null

while IFS=$'\t' read -r rendition_id resolution; do
  output_path="$ARTIFACT_DIR/$resolution.mp4"
  headers_path="$ARTIFACT_DIR/$resolution.headers"
  curl -fsS \
    -D "$headers_path" \
    "$BASE_URL/api/v1/rendition/$rendition_id/download" \
    -o "$output_path"
  grep -qi '^content-type: application/octet-stream' "$headers_path"
  grep -qi '^content-disposition: attachment;' "$headers_path"
  output_height="$(
    ffprobe -v error -select_streams v:0 -show_entries stream=height \
      -of default=noprint_wrappers=1:nokey=1 "$output_path"
  )"
  expected_height="$(jq -nr --arg resolution "$resolution" '$resolution | if . == "SD_360" then 360 else 720 end')"
  if [ "$output_height" != "$expected_height" ]; then
    printf 'Unexpected %s output height: expected %s, got %s\n' \
      "$resolution" "$expected_height" "$output_height" >&2
    exit 1
  fi
done < <(jq -r '.[] | [.id, .resolution] | @tsv' <<< "$RENDITIONS")

NOTIFICATIONS='[]'
for ((attempt = 1; attempt <= 30; attempt++)); do
  NOTIFICATIONS="$(curl -fsS "$BASE_URL/api/v1/notification")"
  if jq -e \
    --arg video_id "$VIDEO_ID" \
    '[.[] | select(.videoId == $video_id)] as $items
      | ($items | map(select(.type == "TASK_COMPLETED")) | length) == 2
        and ($items | map(select(.type == "ALL_COMPLETED")) | length) >= 1' \
    <<< "$NOTIFICATIONS" >/dev/null; then
    break
  fi
  sleep 1
done
printf '%s\n' "$NOTIFICATIONS" > "$ARTIFACT_DIR/notifications.json"
jq -e \
  --arg video_id "$VIDEO_ID" \
  '[.[] | select(.videoId == $video_id)] as $items
    | ($items | map(select(.type == "TASK_COMPLETED")) | length) == 2
      and ($items | map(select(.type == "ALL_COMPLETED")) | length) >= 1' \
  <<< "$NOTIFICATIONS" >/dev/null

for ((attempt = 1; attempt <= 30; attempt++)); do
  if grep -q 'event:notification' "$ARTIFACT_DIR/sse.txt" \
    && grep -q "\"videoId\":\"$VIDEO_ID\"" "$ARTIFACT_DIR/sse.txt"; then
    break
  fi
  sleep 1
done
grep -q 'event:notification' "$ARTIFACT_DIR/sse.txt"
grep -q "\"videoId\":\"$VIDEO_ID\"" "$ARTIFACT_DIR/sse.txt"

printf 'Microservice smoke passed for videoId=%s\nArtifacts: %s\n' "$VIDEO_ID" "$ARTIFACT_DIR"
