package dev.tomato.ripper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Downloader {

    enum DownloadFormat {

        BEST("Best (video)", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best", false),
        Q1080("1080p (video)", "bestvideo[ext=mp4][height<=1080]+bestaudio[ext=m4a]/best[ext=mp4][height<=1080]/best", false),
        Q720("720p (video)", "bestvideo[ext=mp4][height<=720]+bestaudio[ext=m4a]/best[ext=mp4][height<=720]/best", false),
        Q480("480p (video)", "bestvideo[ext=mp4][height<=480]+bestaudio[ext=m4a]/best[ext=mp4][height<=480]/best", false),
        MP3("Audio (MP3)", "bestaudio/best", true);

        private final String label;
        private final String selector;
        private final boolean audio;

        DownloadFormat(String label, String selector, boolean audio) {
            this.label = label;
            this.selector = selector;
            this.audio = audio;
        }

        boolean isAudio() {
            return audio;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    record PreviewInfo(String title, String thumbnailUrl, long durationSeconds) {

        String durationLabel() {
            if (durationSeconds <= 0) {
                return "";
            }
            return durationSeconds / 60 + ":" + String.format("%02d", durationSeconds % 60);
        }
    }

    private static final Pattern PROGRESS_PATTERN = Pattern.compile(
            "\\[download\\]\\s+(\\d+\\.?\\d*)%\\s+of\\s+~?(\\S+)\\s+at\\s+(\\S+)\\s+ETA\\s+(\\S+)");

    private static final Pattern COMPLETE_PATTERN = Pattern.compile(
            "\\[download\\]\\s+100%\\s+of\\s+~?(\\S+)\\s+in\\s+(\\S+)");

    private static final Pattern DEST_PATTERN = Pattern.compile(
            "\\[download\\]\\s+Destination:\\s+(.+)");

    private static final Pattern MERGE_PATTERN = Pattern.compile(
            "\\[Merger\\]\\s+Merging formats into \"(.+)\"");

    private static final Pattern AUDIO_PATTERN = Pattern.compile(
            "\\[ExtractAudio\\]\\s+Destination:\\s+(.+)");

    private Downloader() {
    }

    public static boolean isAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("yt-dlp", "--version");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            int exit = proc.waitFor();
            return exit == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    public static String getVersion() {
        try {
            ProcessBuilder pb = new ProcessBuilder("yt-dlp", "--version");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String version = new String(proc.getInputStream().readAllBytes()).trim();
            proc.waitFor();
            return version.isEmpty() ? "unknown" : version;
        } catch (Exception e) {
            return "unknown";
        }
    }

    static void download(String url, File outputDir, DownloadFormat format,
                         BiConsumer<Double, String> onProgress,
                         Consumer<File> onComplete,
                         Consumer<String> onError) {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            onError.accept("Could not create folder: " + outputDir);
            return;
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("yt-dlp");
        cmd.add("--newline");
        cmd.add("--no-colors");
        cmd.add("--no-playlist");
        cmd.add("--force-overwrites");

        if (format.isAudio()) {
            cmd.add("-x");
            cmd.add("--audio-format");
            cmd.add("mp3");
            cmd.add("--audio-quality");
            cmd.add("0");
        }
        cmd.add("-f");
        cmd.add(format.selector);
        cmd.add("-o");
        cmd.add(outputDir.getAbsolutePath() + "/%(title)s.%(ext)s");
        cmd.add(url);

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            final String[] downloadedFile = new String[1];

            Thread reader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        Matcher progressMatch = PROGRESS_PATTERN.matcher(line);
                        if (progressMatch.find()) {
                            double pct = Double.parseDouble(progressMatch.group(1));
                            onProgress.accept(pct, String.format("Downloading... %.1f%% (%s at %s, ETA %s)",
                                    pct, progressMatch.group(2), progressMatch.group(3), progressMatch.group(4)));
                            continue;
                        }

                        Matcher completeMatch = COMPLETE_PATTERN.matcher(line);
                        if (completeMatch.find()) {
                            onProgress.accept(100.0, "Download complete, processing...");
                            continue;
                        }

                        Matcher audioMatch = AUDIO_PATTERN.matcher(line);
                        if (audioMatch.find()) {
                            downloadedFile[0] = audioMatch.group(1).trim();
                            continue;
                        }

                        Matcher mergeMatch = MERGE_PATTERN.matcher(line);
                        if (mergeMatch.find()) {
                            downloadedFile[0] = mergeMatch.group(1).trim();
                            continue;
                        }

                        Matcher destMatch = DEST_PATTERN.matcher(line);
                        if (destMatch.find()) {
                            downloadedFile[0] = destMatch.group(1).trim();
                        }
                    }
                } catch (IOException e) {
                    onError.accept("Read error: " + e.getMessage());
                }
            }, "yt-dlp-reader");
            reader.setDaemon(true);
            reader.start();

            int exitCode = proc.waitFor();
            reader.join(5000);

            if (exitCode == 0 && downloadedFile[0] != null) {
                File file = new File(downloadedFile[0]);
                if (file.exists()) {
                    onComplete.accept(file);
                } else {
                    onError.accept("Download completed but file not found: " + downloadedFile[0]);
                }
            } else if (exitCode != 0) {
                onError.accept("yt-dlp exited with code " + exitCode);
            } else {
                onError.accept("Could not determine downloaded file path");
            }

        } catch (IOException e) {
            onError.accept("Failed to start yt-dlp: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            onError.accept("Download interrupted");
        }
    }

    static void preview(String url, Consumer<PreviewInfo> onResult, Consumer<String> onError) {
        List<String> cmd = List.of(
                "yt-dlp",
                "--skip-download",
                "--no-playlist",
                "--no-warnings",
                "--print", "TITLE=%(title)s",
                "--print", "THUMB=%(thumbnail)s",
                "--print", "DUR=%(duration)s",
                url
        );
        try {
            Process proc = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            byte[] raw = proc.getInputStream().readAllBytes();
            int exit = proc.waitFor();
            if (exit != 0) {
                onError.accept("Could not fetch video info (is the link valid?)");
                return;
            }
            PreviewInfo info = parsePreview(new String(raw));
            if (info == null) {
                onError.accept("No video information found for this link");
            } else {
                onResult.accept(info);
            }
        } catch (IOException e) {
            onError.accept("Failed to start yt-dlp: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            onError.accept("Fetch interrupted");
        }
    }

    private static PreviewInfo parsePreview(String out) {
        String title = null;
        String thumb = null;
        long duration = 0;
        for (String line : out.split("\n")) {
            line = line.trim();
            if (line.startsWith("TITLE=")) {
                title = line.substring("TITLE=".length()).trim();
            } else if (line.startsWith("THUMB=")) {
                String t = line.substring("THUMB=".length()).trim();
                if (!t.isEmpty() && !t.equalsIgnoreCase("None") && !t.equalsIgnoreCase("NA")) {
                    thumb = t;
                }
            } else if (line.startsWith("DUR=")) {
                try {
                    duration = (long) Double.parseDouble(line.substring("DUR=".length()).trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (title == null || title.isEmpty()) {
            return null;
        }
        return new PreviewInfo(title, thumb, duration);
    }
}