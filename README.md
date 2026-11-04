# Tomato Media Ripper

Desktop GUI for downloading video and audio from Twitter/X, TikTok, Instagram,
and Facebook. It wraps [yt-dlp](https://github.com/yt-dlp/yt-dlp) with a dark
Swing interface, a pre-download preview, multiple quality presets, async
downloads with progress tracking, and an in-memory download history.

## Requirements

- Java 21 or newer
- [yt-dlp](https://github.com/yt-dlp/yt-dlp) on your `PATH`
- FFmpeg (recommended; required for GIF conversion)

## Usage

```bash
./tomato-media-ripper.sh
```

The launcher runs `tomato-media-ripper.jar` if it is present, otherwise it
compiles the sources in `src/main/java` and runs them directly.

To build a jar yourself:

```bash
mkdir -p out
find src/main/java -name '*.java' > sources.txt
javac -d out @sources.txt
jar --create --file tomato-media-ripper.jar --main-class dev.tomato.ripper.Main -C out .
```

## Features

- URL input with platform detection and a preview panel (title, thumbnail,
  duration)
- Video and audio format presets, including animated GIF output via FFmpeg
- Async downloads with a progress bar
- Download history for the current session (stored in memory, not on disk)
- Save-folder preference persisted in
  `~/.config/tomato-media-ripper/settings.properties`

## Notes

Download only content you have the right to download. The included presets use
`--force-overwrites`; remove that flag in `Downloader.java` if you prefer to be
prompted before overwriting files.
