package dev.local.ai.ui.chat.controls;

/**
 * JavaFX WebKit draws a stray .notdef box next to emoji (especially in bold
 * headings). Replacing emoji with Twemoji PNGs avoids font shaping entirely.
 */
final class HtmlEmojiWrapper {

    /** Relative to {@code /chat/chat.html} in classpath (see ui build: twemoji assets). */
    private static final String TWEMOJI_RESOURCE_PREFIX = "emoji/";

    /** Used when a PNG is missing from the bundled set (requires network). */
    private static final String TWEMOJI_CDN_FALLBACK =
            "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/";

    private HtmlEmojiWrapper() {
    }

    static String wrap(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        StringBuilder result = new StringBuilder(html.length() + 64);
        int pos = 0;
        while (pos < html.length()) {
            int tagStart = html.indexOf('<', pos);
            if (tagStart < 0) {
                result.append(wrapText(html.substring(pos)));
                break;
            }
            if (tagStart > pos) {
                result.append(wrapText(html.substring(pos, tagStart)));
            }
            int tagEnd = html.indexOf('>', tagStart);
            if (tagEnd < 0) {
                result.append(html.substring(tagStart));
                break;
            }
            result.append(html, tagStart, tagEnd + 1);
            pos = tagEnd + 1;
        }
        return result.toString();
    }

    private static String wrapText(String text) {
        StringBuilder out = new StringBuilder(text.length() + 48);
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            if (isEmojiBase(cp)) {
                int runEnd = endOfEmojiRun(text, i);
                out.append(toEmojiImg(text.substring(i, runEnd)));
                i = runEnd;
            } else {
                out.appendCodePoint(cp);
                i += Character.charCount(cp);
            }
        }
        return out.toString();
    }

    private static String toEmojiImg(String emojiRun) {
        String assetId = twemojiAssetId(emojiRun);
        String src = TWEMOJI_RESOURCE_PREFIX + assetId + ".png";
        return "<img class=\"emoji-img\" src=\"" + src + "\" alt=\"" + escapeAttr(emojiRun)
                + "\" data-twemoji-id=\"" + assetId + "\""
                + " onerror=\"if(!this.dataset.fallback){this.dataset.fallback='1';"
                + "this.src='" + TWEMOJI_CDN_FALLBACK + "'+this.dataset.twemojiId+'.png';}\""
                + " draggable=\"false\" loading=\"lazy\">";
    }

    /** Twemoji file name without extension (e.g. {@code 1f4a1} or {@code 1f468-200d-1f469}). */
    static String twemojiAssetId(String emojiRun) {
        StringBuilder hex = new StringBuilder();
        emojiRun.codePoints()
                .filter(cp -> cp != 0xFE0F && cp != 0xFE0E)
                .forEach(cp -> {
                    if (hex.length() > 0) {
                        hex.append('-');
                    }
                    hex.append(Integer.toHexString(cp));
                });
        return hex.toString();
    }

    private static String escapeAttr(String value) {
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;");
    }

    private static int endOfEmojiRun(String text, int start) {
        int i = start + Character.charCount(text.codePointAt(start));
        int len = text.length();
        while (i < len) {
            int cp = text.codePointAt(i);
            if (cp == 0xFE0F || cp == 0xFE0E) {
                i += Character.charCount(cp);
                continue;
            }
            if (cp == 0x200D) {
                i += Character.charCount(cp);
                if (i < len && isEmojiBase(text.codePointAt(i))) {
                    i += Character.charCount(text.codePointAt(i));
                    continue;
                }
                break;
            }
            if (cp >= 0x1F3FB && cp <= 0x1F3FF) {
                i += Character.charCount(cp);
                continue;
            }
            if (cp == 0x20E3 && i > start) {
                i += Character.charCount(cp);
                continue;
            }
            break;
        }
        return i;
    }

    private static boolean isEmojiBase(int cp) {
        if (cp >= 0x1F1E6 && cp <= 0x1F1FF) {
            return true;
        }
        if (cp >= 0x1F3FB && cp <= 0x1F3FF) {
            return true;
        }
        Character.UnicodeBlock block = Character.UnicodeBlock.of(cp);
        return block == Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS
                || block == Character.UnicodeBlock.DINGBATS
                || block == Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS
                || block == Character.UnicodeBlock.EMOTICONS
                || block == Character.UnicodeBlock.TRANSPORT_AND_MAP_SYMBOLS
                || block == Character.UnicodeBlock.ALCHEMICAL_SYMBOLS
                || block == Character.UnicodeBlock.GEOMETRIC_SHAPES_EXTENDED
                || block == Character.UnicodeBlock.SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS
                || block == Character.UnicodeBlock.SYMBOLS_AND_PICTOGRAPHS_EXTENDED_A
                || block == Character.UnicodeBlock.MAHJONG_TILES
                || block == Character.UnicodeBlock.PLAYING_CARDS
                || block == Character.UnicodeBlock.ENCLOSED_ALPHANUMERIC_SUPPLEMENT
                || block == Character.UnicodeBlock.ENCLOSED_IDEOGRAPHIC_SUPPLEMENT;
    }
}
