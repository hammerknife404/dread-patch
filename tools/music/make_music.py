"""Makes the Dreadlands' music from the owner's recording, into src/main/resources.

Python 3 (stdlib) and ffmpeg with libvorbis.

    python3 tools/music/make_music.py ["custom_music/Entombed In Rimefrost.mp4"]

The source isn't in the repository (a 9 MB video). Two Ogg Vorbis files come out of it, both brought to
TARGET_LUFS by a plain gain (so its dynamics are kept; the recording is about -12.4 LUFS, vanilla's
music about -15 to -16.5, and the owner asked for it not too loud):

- sounds/music/entombed_in_rimefrost.ogg, stereo: the Dreadlands' background music;
- sounds/records/entombed_in_rimefrost.ogg, mono: the music disc. A jukebox's sound fades with distance
  only if it's mono (OpenAL plays stereo at full volume wherever the listener is).

It prints each file's length: the jukebox song's length_in_seconds must cover it.
"""
import pathlib
import re
import subprocess
import sys

REPO = pathlib.Path(__file__).resolve().parents[2]
SOUNDS = REPO / "src/main/resources/assets/dreadpatch/sounds"
SOURCE = REPO / "custom_music/Entombed In Rimefrost.mp4"
NAME = "entombed_in_rimefrost"
TARGET_LUFS = -17.0
# Vorbis quality: 4 is about 128 kb/s stereo, vanilla's own music is near it.
QUALITY = {"music": 4, "records": 3}
# Downmixed in the filter, before the loudness meter (an -ac output option would act after it).
LAYOUT = {1: "mono", 2: "stereo"}


def loudness(source, channels):
    """Integrated loudness (LUFS) of the source, downmixed to `channels`."""
    out = subprocess.run(
        ["ffmpeg", "-hide_banner", "-nostats", "-i", str(source), "-vn",
         "-af", f"aformat=channel_layouts={LAYOUT[channels]},ebur128", "-f", "null", "-"],
        capture_output=True, text=True, check=True).stderr
    summary = out[out.rindex("Summary:"):]
    return float(re.search(r"I:\s+(-?[\d.]+) LUFS", summary).group(1))


def length(path):
    out = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(path)],
                         capture_output=True, text=True, check=True).stdout
    return float(out.strip())


def make(source, folder, channels):
    gain = TARGET_LUFS - loudness(source, channels)
    target = SOUNDS / folder / f"{NAME}.ogg"
    target.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(source), "-vn", "-map_metadata", "-1",
         "-ar", "44100", "-af", f"aformat=channel_layouts={LAYOUT[channels]},volume={gain:.2f}dB",
         "-c:a", "libvorbis", "-q:a", str(QUALITY[folder]), "-fflags", "+bitexact", "-flags:a", "+bitexact",
         str(target)],
        check=True)
    print(f"{target.relative_to(REPO)}: {channels} ch, {gain:+.2f} dB, now {loudness(target, channels):.1f} LUFS, "
          f"{length(target):.2f} s, {target.stat().st_size // 1024} KiB")


def main():
    source = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else SOURCE
    make(source, "music", 2)
    make(source, "records", 1)


if __name__ == "__main__":
    main()
