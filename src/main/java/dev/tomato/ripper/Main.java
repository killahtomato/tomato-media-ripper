package dev.tomato.ripper;

public class Main {

    public static void main(String[] args) {
        if (!Downloader.isAvailable()) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                javax.swing.JOptionPane.showMessageDialog(
                        null,
                        "<html><div style='width:350px;'>"
                                + "<b>yt-dlp is not installed</b><br><br>"
                                + "This application requires yt-dlp to download videos.<br><br>"
                                + "Install it with:<br>"
                                + "<code>pip install yt-dlp</code><br><br>"
                                + "FFmpeg is also recommended for merging audio and video streams:<br>"
                                + "<code>sudo apt install ffmpeg</code>"
                                + "</div></html>",
                        "Tomato Media Ripper - Missing Dependency",
                        javax.swing.JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);
            });
            return;
        }

        System.out.println("yt-dlp version: " + Downloader.getVersion());
        System.out.println("Launching Tomato Media Ripper...");

        Gui.launch();
    }
}