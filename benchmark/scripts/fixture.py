#!/usr/bin/env python3
import hashlib
import json
import os
import subprocess
import sys
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[2]
RECIPE_PATH = Path(
    os.environ.get(
        "BENCHMARK_FIXTURE_RECIPE", ROOT_DIR / "benchmark/fixtures/source-video.recipe.json"
    )
).resolve()
MANIFEST_PATH = Path(
    os.environ.get(
        "BENCHMARK_FIXTURE_MANIFEST", ROOT_DIR / "benchmark/fixtures/source-video.manifest.json"
    )
).resolve()


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as input_file:
        for chunk in iter(lambda: input_file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def load_json(path: Path) -> dict:
    with path.open(encoding="utf-8") as input_file:
        return json.load(input_file)


def docker_command(recipe: dict) -> list[str]:
    output_path = ROOT_DIR / recipe["output"]
    return [
        "docker",
        "run",
        "--rm",
        "--user",
        f"{os.getuid()}:{os.getgid()}",
        "--volume",
        f"{output_path.parent}:/work",
        recipe["containerImage"],
        *recipe["ffmpegArguments"],
    ]


def probe(path: Path, image: str) -> dict:
    command = [
        "docker",
        "run",
        "--rm",
        "--entrypoint",
        "ffprobe",
        "--volume",
        f"{path.parent}:/work:ro",
        image,
        "-v",
        "error",
        "-select_streams",
        "v:0",
        "-show_entries",
        "stream=width,height,r_frame_rate",
        "-show_entries",
        "format=duration",
        "-of",
        "json",
        f"/work/{path.name}",
    ]
    completed = subprocess.run(command, check=True, text=True, capture_output=True)
    return json.loads(completed.stdout)


def generate() -> None:
    recipe = load_json(RECIPE_PATH)
    if "@sha256:" not in recipe["containerImage"]:
        raise SystemExit("Fixture generator container must be pinned by digest")
    output_path = ROOT_DIR / recipe["output"]
    output_path.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(docker_command(recipe), check=True)
    probed = probe(output_path, recipe["containerImage"])
    stream = probed["streams"][0]
    manifest = {
        "schemaVersion": 1,
        "fixtureId": recipe["fixtureId"],
        "materialized": True,
        "recipe": str(RECIPE_PATH.relative_to(ROOT_DIR)),
        "recipeSha256": sha256(RECIPE_PATH),
        "file": recipe["output"],
        "sha256": sha256(output_path),
        "containerImage": recipe["containerImage"],
        "width": int(stream["width"]),
        "height": int(stream["height"]),
        "durationSeconds": float(probed["format"]["duration"]),
        "framesPerSecond": recipe["framesPerSecond"],
    }
    MANIFEST_PATH.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Generated {output_path}")
    print(f"Recorded SHA-256 provenance in {MANIFEST_PATH}")


def verify() -> None:
    recipe = load_json(RECIPE_PATH)
    manifest = load_json(MANIFEST_PATH)
    if "@sha256:" not in recipe["containerImage"]:
        raise SystemExit("Fixture generator container must be pinned by digest")
    if not manifest.get("materialized") or not manifest.get("sha256"):
        raise SystemExit(
            "Fixture manifest is not materialized. Run benchmark/scripts/generate-video.sh explicitly, "
            "review the manifest, and commit only the JSON provenance for an official protocol."
        )
    if manifest.get("recipeSha256") != sha256(RECIPE_PATH):
        raise SystemExit("Fixture recipe SHA-256 does not match the materialized manifest")
    if manifest.get("containerImage") != recipe["containerImage"]:
        raise SystemExit("Fixture generator image does not match the recipe")
    if manifest.get("file") != recipe["output"]:
        raise SystemExit("Fixture output path does not match the recipe")
    output_path = ROOT_DIR / manifest["file"]
    if not output_path.is_file():
        raise SystemExit(f"Fixture is missing: {output_path}")
    actual_sha256 = sha256(output_path)
    if actual_sha256 != manifest["sha256"]:
        raise SystemExit(
            f"Fixture SHA-256 mismatch: expected {manifest['sha256']}, got {actual_sha256}"
        )
    probed = probe(output_path, manifest["containerImage"])
    stream = probed["streams"][0]
    if int(stream["height"]) <= 720:
        raise SystemExit(f"Fixture height must be greater than 720, got {stream['height']}")
    if int(stream["width"]) != int(manifest["width"]) or int(stream["height"]) != int(
        manifest["height"]
    ):
        raise SystemExit("Fixture dimensions do not match the manifest")
    print(f"Verified fixture {output_path} sha256={actual_sha256}")


def print_command() -> None:
    import shlex

    print(shlex.join(docker_command(load_json(RECIPE_PATH))))


def main() -> int:
    action = sys.argv[1] if len(sys.argv) > 1 else "verify"
    if action == "generate":
        generate()
    elif action == "verify":
        verify()
    elif action == "print-command":
        print_command()
    else:
        raise SystemExit("Usage: fixture.py [generate|verify|print-command]")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
