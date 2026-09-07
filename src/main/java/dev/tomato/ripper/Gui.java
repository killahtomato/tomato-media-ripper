package dev.tomato.ripper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

final class Gui {

    private static final Color BG = new Color(0x10121a);
    private static final Color CARD_FILL = new Color(0x1d1f2b);
    private static final Color CARD_EDGE = new Color(0x2a2f3a);
    private static final Color TEXT = new Color(0xdfe3ea);
    private static final Color MUTED = new Color(0x8891a5);
    private static final Color ACCENT = new Color(0x6ea3ff);
    private static final Color GREEN = new Color(0x4ade80);
    private static final Color RED = new Color(0xf87171);
    private static final Color INPUT_BG = new Color(0x1a1c28);
    private static final Color INPUT_BORDER = new Color(0x3a3f4e);

    private static final int WINDOW_WIDTH = 840;
    private static final int WINDOW_HEIGHT = 660;
    private static final int GAP = 10;

    private static final File SETTINGS_DIR = new File(
            System.getProperty("user.home"), ".config/tomato-media-ripper");
    private static final File SETTINGS_FILE = new File(SETTINGS_DIR, "settings.properties");
    private static final String DEFAULT_FOLDER = "Downloads/tomato-media-ripper";

    private static JFrame frame;
    private static JTextField urlField;
    private static JTextField folderField;
    private static JComboBox<Downloader.DownloadFormat> formatCombo;
    private static JButton downloadBtn;
    private static JButton infoBtn;
    private static JLabel statusLabel;
    private static JProgressBar progressBar;
    private static JPanel previewPanel;
    private static JPanel historyPanel;
    private static final List<DownloadEntry> history = new ArrayList<>();

    private Gui() {
    }

    static void launch() {
        SwingUtilities.invokeLater(Gui::buildAndShow);
    }

    private static void buildAndShow() {
        frame = new JFrame("Tomato Media Ripper");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setMinimumSize(new Dimension(600, 480));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(GAP, GAP, GAP, GAP));
        frame.setContentPane(root);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, GAP, 0));

        JLabel title = new JLabel("Tomato's Audio/Video Ripper");
        title.setForeground(TEXT);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        header.add(title, BorderLayout.WEST);

        JLabel version = new JLabel("yt-dlp " + Downloader.getVersion());
        version.setForeground(MUTED);
        version.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        header.add(version, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        center.add(createInputPanel());
        center.add(Box.createVerticalStrut(8));
        center.add(createOptionsPanel());
        center.add(Box.createVerticalStrut(GAP));
        center.add(createPreviewPanel());
        center.add(Box.createVerticalStrut(GAP));
        center.add(createProgressPanel());
        center.add(Box.createVerticalStrut(GAP));
        center.add(createHistoryHeader());
        center.add(Box.createVerticalStrut(4));
        center.add(createHistoryScroll());

        root.add(center, BorderLayout.CENTER);

        frame.setVisible(true);
        urlField.requestFocusInWindow();
    }

    private static JPanel createInputPanel() {
        urlField = new JTextField();
        urlField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        urlField.setBackground(INPUT_BG);
        urlField.setForeground(TEXT);
        urlField.setCaretColor(TEXT);
        urlField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1),
                new EmptyBorder(9, 10, 9, 10)
        ));
        urlField.setToolTipText("Paste a video URL from Twitter, TikTok, Instagram, or Facebook");

        urlField.addActionListener(e -> startDownload());

        JButton pasteBtn = smallButton("Paste");
        pasteBtn.setToolTipText("Paste the URL from your clipboard");
        pasteBtn.addActionListener(e -> pasteFromClipboard());

        infoBtn = smallButton("Get Info");
        infoBtn.setToolTipText("Preview title, thumbnail and duration without downloading");
        infoBtn.addActionListener(e -> fetchPreview());

        downloadBtn = accentButton("Download");
        downloadBtn.addActionListener(e -> startDownload());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setOpaque(false);
        buttons.add(pasteBtn);
        buttons.add(infoBtn);
        buttons.add(downloadBtn);

        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.add(urlField, BorderLayout.CENTER);
        row.add(buttons, BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(row, BorderLayout.CENTER);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        return wrapper;
    }

    private static JPanel createOptionsPanel() {
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.setOpaque(false);

        JLabel formatLabel = new JLabel("Format:");
        formatLabel.setForeground(MUTED);
        formatLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        left.add(formatLabel);

        formatCombo = new JComboBox<>(Downloader.DownloadFormat.values());
        formatCombo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        formatCombo.setBackground(INPUT_BG);
        formatCombo.setForeground(TEXT);
        formatCombo.setFocusable(false);
        formatCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean selected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, selected, cellHasFocus);
                setBackground(selected ? ACCENT : INPUT_BG);
                setForeground(selected ? Color.WHITE : TEXT);
                return this;
            }
        });
        left.add(formatCombo);

        JLabel saveLabel = new JLabel("  Save to:");
        saveLabel.setForeground(MUTED);
        saveLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        left.add(saveLabel);

        JPanel right = new JPanel(new BorderLayout(6, 0));
        right.setOpaque(false);

        folderField = new JTextField();
        folderField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        folderField.setBackground(INPUT_BG);
        folderField.setForeground(TEXT);
        folderField.setCaretColor(TEXT);
        folderField.setBorder(BorderFactory.createLineBorder(INPUT_BORDER, 1));
        folderField.setEditable(false);
        folderField.setText(loadSaveFolder().getAbsolutePath());
        right.add(folderField, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        btns.setOpaque(false);

        JButton browseBtn = smallButton("Browse");
        browseBtn.addActionListener(e -> chooseSaveFolder());
        btns.add(browseBtn);

        JButton defaultBtn = smallButton("Default");
        defaultBtn.setToolTipText("Reset to " + DEFAULT_FOLDER);
        defaultBtn.addActionListener(e -> {
            File def = defaultSaveFolder();
            folderField.setText(def.getAbsolutePath());
            persistSaveFolder(def);
        });
        btns.add(defaultBtn);

        right.add(btns, BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(left, BorderLayout.WEST);
        wrapper.add(right, BorderLayout.CENTER);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        return wrapper;
    }

    private static JPanel createPreviewPanel() {
        previewPanel = new JPanel(new BorderLayout());
        previewPanel.setOpaque(true);
        previewPanel.setBackground(CARD_FILL);
        previewPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_EDGE, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));
        showPreviewHint();

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(previewPanel, BorderLayout.CENTER);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        return wrapper;
    }

    private static void showPreviewHint() {
        previewPanel.removeAll();
        JLabel hint = new JLabel("Enter a link and click 'Get Info' to preview it before downloading",
                SwingConstants.CENTER);
        hint.setForeground(MUTED);
        hint.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        previewPanel.add(hint, BorderLayout.CENTER);
        previewPanel.revalidate();
        previewPanel.repaint();
    }

    private static void updatePreview(Downloader.PreviewInfo info, ImageIcon icon) {
        previewPanel.removeAll();

        JPanel card = new JPanel(new BorderLayout(GAP, 0));
        card.setOpaque(false);

        JLabel thumb;
        if (icon != null) {
            thumb = new JLabel(icon);
        } else {
            thumb = new JLabel("no preview", SwingConstants.CENTER);
            thumb.setPreferredSize(new Dimension(150, 84));
            thumb.setOpaque(true);
            thumb.setBackground(INPUT_BG);
            thumb.setForeground(MUTED);
            thumb.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        }
        card.add(thumb, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("<html>" + esc(info.title()) + "</html>");
        titleLabel.setForeground(TEXT);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        titleLabel.setAlignmentX(0f);
        text.add(titleLabel);

        StringBuilder meta = new StringBuilder();
        String dur = info.durationLabel();
        if (!dur.isEmpty()) {
            meta.append(dur).append("  \u00b7  ");
        }
        Platform p = Platform.fromUrl(urlField.getText());
        if (p != null) {
            meta.append(p.getDisplayName());
        }
        if (meta.length() > 0) {
            JLabel metaLabel = new JLabel(meta.toString().trim());
            metaLabel.setForeground(MUTED);
            metaLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            metaLabel.setAlignmentX(0f);
            metaLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
            text.add(metaLabel);

            text.add(Box.createVerticalGlue());
        }
        card.add(text, BorderLayout.CENTER);

        previewPanel.add(card, BorderLayout.CENTER);
        previewPanel.revalidate();
        previewPanel.repaint();
    }

    private static JPanel createProgressPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(true);
        panel.setBackground(CARD_FILL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_EDGE, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setBackground(INPUT_BG);
        progressBar.setForeground(ACCENT);
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 8));
        panel.add(progressBar, BorderLayout.NORTH);

        statusLabel = new JLabel("Ready to download");
        statusLabel.setForeground(MUTED);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        panel.add(statusLabel, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(panel, BorderLayout.CENTER);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        return wrapper;
    }

    private static JLabel createHistoryHeader() {
        JLabel label = new JLabel("Download History");
        label.setForeground(MUTED);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        return label;
    }

    private static JScrollPane createHistoryScroll() {
        historyPanel = new JPanel();
        historyPanel.setOpaque(false);
        historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));

        JLabel empty = new JLabel("No downloads yet");
        empty.setForeground(MUTED);
        empty.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        empty.setAlignmentX(0f);
        historyPanel.add(empty);

        JScrollPane scroll = new JScrollPane(historyPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private static String normalizeUrl() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            return null;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
            urlField.setText(url);
        }
        return url;
    }

    private static File currentSaveFolder() {
        String text = folderField.getText().trim();
        return text.isEmpty() ? defaultSaveFolder() : new File(text);
    }

    private static void startDownload() {
        String url = normalizeUrl();
        if (url == null) {
            setStatus("Please enter a video URL", RED);
            return;
        }

        File folder = currentSaveFolder();
        Downloader.DownloadFormat format = (Downloader.DownloadFormat) formatCombo.getSelectedItem();

        downloadBtn.setEnabled(false);
        downloadBtn.setText("Downloading...");
        progressBar.setValue(0);
        setStatus("Starting download...", TEXT);

        new DownloadWorker(url, folder, format).execute();
    }

    private static class DownloadWorker extends SwingWorker<File, ProgressUpdate> {

        private final String url;
        private final File outputDir;
        private final Downloader.DownloadFormat format;

        DownloadWorker(String url, File outputDir, Downloader.DownloadFormat format) {
            this.url = url;
            this.outputDir = outputDir;
            this.format = format;
        }

        @Override
        protected File doInBackground() {
            CountDownLatch latch = new CountDownLatch(1);
            final File[] result = new File[1];
            final String[] error = new String[1];

            Downloader.download(url, outputDir, format,
                    (pct, text) -> publish(new ProgressUpdate(pct, text)),
                    file -> {
                        result[0] = file;
                        latch.countDown();
                    },
                    err -> {
                        error[0] = err;
                        publish(new ProgressUpdate(-1, "Error: " + err));
                        latch.countDown();
                    });

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            if (result[0] == null && error[0] != null) {
                throw new DownloadException(error[0]);
            }
            return result[0];
        }

        @Override
        protected void process(List<ProgressUpdate> chunks) {
            ProgressUpdate latest = chunks.get(chunks.size() - 1);
            if (latest.percentage >= 0) {
                progressBar.setValue((int) latest.percentage);
                setStatus(latest.text, TEXT);
            } else {
                setStatus(latest.text, RED);
                progressBar.setValue(0);
            }
        }

        @Override
        protected void done() {
            try {
                File file = get();
                progressBar.setValue(100);
                setStatus("Downloaded: " + file.getName(), GREEN);
                addHistoryEntry(url, file);
                openFolder(file.getParentFile());
            } catch (Exception e) {
                String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                setStatus("Download failed: " + msg, RED);
                progressBar.setValue(0);
            }

            downloadBtn.setEnabled(true);
            downloadBtn.setText("Download");
        }
    }

    private record ProgressUpdate(double percentage, String text) {
    }

    private static class DownloadException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        DownloadException(String message) {
            super(message);
        }
    }

    private static void fetchPreview() {
        String url = normalizeUrl();
        if (url == null) {
            setStatus("Please enter a video URL", RED);
            return;
        }

        infoBtn.setEnabled(false);
        setStatus("Fetching video info...", MUTED);

        new PreviewWorker(url).execute();
    }

    private static class PreviewWorker extends SwingWorker<Object[], Void> {

        private final String url;

        PreviewWorker(String url) {
            this.url = url;
        }

        @Override
        protected Object[] doInBackground() {
            CountDownLatch latch = new CountDownLatch(1);
            final Downloader.PreviewInfo[] info = new Downloader.PreviewInfo[1];
            final String[] error = new String[1];

            Downloader.preview(url,
                    i -> {
                        info[0] = i;
                        latch.countDown();
                    },
                    e -> {
                        error[0] = e;
                        latch.countDown();
                    });

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new Object[]{null, null, "Fetch interrupted"};
            }

            ImageIcon icon = null;
            if (info[0] != null && info[0].thumbnailUrl() != null) {
                icon = loadThumbnail(info[0].thumbnailUrl());
            }
            return new Object[]{info[0], icon, error[0]};
        }

        @Override
        protected void done() {
            try {
                Object[] res = get();
                Downloader.PreviewInfo info = (Downloader.PreviewInfo) res[0];
                ImageIcon icon = (ImageIcon) res[1];
                String error = (String) res[2];

                if (info != null) {
                    updatePreview(info, icon);
                    setStatus((icon == null ? "No thumbnail available" : "Preview ready") + " - click Download to save", TEXT);
                } else {
                    showPreviewHint();
                    setStatus("Preview failed: " + error, RED);
                }
            } catch (Exception e) {
                showPreviewHint();
                String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                setStatus("Preview failed: " + msg, RED);
            } finally {
                infoBtn.setEnabled(true);
            }
        }
    }

    private static ImageIcon loadThumbnail(String url) {
        try {
            BufferedImage img = ImageIO.read(java.net.URI.create(url).toURL());
            if (img == null) {
                return null;
            }
            int w = 150;
            int h = (int) (img.getHeight() * (double) w / img.getWidth());
            if (h > 84) {
                h = 84;
                w = (int) (img.getWidth() * (double) h / img.getHeight());
            }
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }

    private static void pasteFromClipboard() {
        try {
            String clip = (String) Toolkit.getDefaultToolkit().getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            if (clip != null && !clip.isBlank()) {
                urlField.setText(clip.trim());
                setStatus("Pasted from clipboard", MUTED);
            } else {
                setStatus("Clipboard is empty", MUTED);
            }
        } catch (Exception e) {
            setStatus("Could not read clipboard", RED);
        }
    }

    private static File defaultSaveFolder() {
        return new File(System.getProperty("user.home"), DEFAULT_FOLDER);
    }

    private static File loadSaveFolder() {
        try {
            if (SETTINGS_FILE.exists()) {
                Properties p = new Properties();
                try (FileInputStream in = new FileInputStream(SETTINGS_FILE)) {
                    p.load(in);
                }
                String path = p.getProperty("saveFolder");
                if (path != null && !path.isBlank()) {
                    return new File(path);
                }
            }
        } catch (Exception e) {
            System.err.println("warn: could not load settings (" + e + ")");
        }
        return defaultSaveFolder();
    }

    private static void persistSaveFolder(File folder) {
        try {
            SETTINGS_DIR.mkdirs();
            Properties p = new Properties();
            if (SETTINGS_FILE.exists()) {
                try (FileInputStream in = new FileInputStream(SETTINGS_FILE)) {
                    p.load(in);
                }
            }
            p.setProperty("saveFolder", folder.getAbsolutePath());
            try (FileOutputStream out = new FileOutputStream(SETTINGS_FILE)) {
                p.store(out, "Tomato Media Ripper settings");
            }
        } catch (Exception e) {
            System.err.println("warn: could not save settings (" + e + ")");
        }
    }

    private static void chooseSaveFolder() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose save folder");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setCurrentDirectory(currentSaveFolder());
        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File folder = fc.getSelectedFile();
            folderField.setText(folder.getAbsolutePath());
            persistSaveFolder(folder);
            setStatus("Save folder: " + folder.getAbsolutePath(), MUTED);
        }
    }

    private static void addHistoryEntry(String url, File file) {
        history.add(0, new DownloadEntry(url, file));

        if (historyPanel.getComponentCount() == 1
                && historyPanel.getComponent(0) instanceof JLabel
                && ((JLabel) historyPanel.getComponent(0)).getText().equals("No downloads yet")) {
            historyPanel.removeAll();
        }

        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(6, 8, 6, 8));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel(file.getName());
        nameLabel.setForeground(TEXT);
        nameLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        nameLabel.setAlignmentX(0f);
        left.add(nameLabel);

        Platform platform = Platform.fromUrl(url);
        JLabel metaLabel = new JLabel(platform != null ? platform.getDisplayName() : "Unknown");
        metaLabel.setForeground(MUTED);
        metaLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        metaLabel.setAlignmentX(0f);
        left.add(metaLabel);

        card.add(left, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setOpaque(false);

        JButton openBtn = smallButton("Open");
        openBtn.addActionListener(e -> openFile(file));
        buttons.add(openBtn);

        JButton folderBtn = smallButton("Show in folder");
        folderBtn.addActionListener(e -> openFolder(file.getParentFile()));
        buttons.add(folderBtn);

        card.add(buttons, BorderLayout.EAST);

        historyPanel.add(card, 0);
        historyPanel.revalidate();
        historyPanel.repaint();
    }

    private static JButton smallButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btn.setBackground(CARD_EDGE);
        btn.setForeground(MUTED);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 10, 6, 10));
        return btn;
    }

    private static JButton accentButton(String text) {
        JButton btn = smallButton(text);
        btn.setBackground(ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        return btn;
    }

    private static void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private static void openFile(File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            setStatus("Could not open file: " + e.getMessage(), RED);
        }
    }

    private static void openFolder(File folder) {
        try {
            Desktop.getDesktop().open(folder);
        } catch (IOException e) {
            setStatus("Could not open folder: " + e.getMessage(), RED);
        }
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private record DownloadEntry(String url, File file) {
    }
}