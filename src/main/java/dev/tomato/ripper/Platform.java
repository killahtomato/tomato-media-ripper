package dev.tomato.ripper;

enum Platform {

    TWITTER("Twitter / X", "twitter.com", "x.com", "nitter.net"),
    TIKTOK("TikTok", "tiktok.com"),
    INSTAGRAM("Instagram", "instagram.com"),
    FACEBOOK("Facebook", "facebook.com", "fb.watch");

    private final String displayName;
    private final String[] domains;

    Platform(String displayName, String... domains) {
        this.displayName = displayName;
        this.domains = domains;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Platform fromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String lower = url.toLowerCase();
        for (Platform p : values()) {
            for (String domain : p.domains) {
                if (lower.contains(domain)) {
                    return p;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return displayName;
    }
}