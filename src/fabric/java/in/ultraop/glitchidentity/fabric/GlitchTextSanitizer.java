package in.ultraop.glitchidentity.fabric;

import in.ultraop.glitchidentity.core.GlitchColorMode;
import in.ultraop.glitchidentity.core.GlitchFrameGenerator;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;

import java.util.Arrays;
import java.util.Objects;

public final class GlitchTextSanitizer {
    public static final String VICTIM_MARKER = "\uE000GIV\uE001";
    public static final String KILLER_MARKER = "\uE000GIK\uE001";
    public static final String VICTIM_WHITE_MARKER = "\uE000WIV\uE001";
    public static final String KILLER_WHITE_MARKER = "\uE000WIK\uE001";

    private GlitchTextSanitizer() {}

    public static Text sanitize(Text source, String victimName, String killerName) {
        return sanitize(
            source,
            victimName,
            killerName,
            GlitchColorMode.COLORFUL,
            GlitchColorMode.COLORFUL
        );
    }

    public static Text sanitize(
        Text source,
        String victimName,
        String killerName,
        GlitchColorMode victimMode,
        GlitchColorMode killerMode
    ) {
        return sanitizeText(
            source,
            victimName,
            killerName,
            victimMode,
            killerMode
        );
    }

    private static Text sanitizeText(
        Text source,
        String victimName,
        String killerName,
        GlitchColorMode victimMode,
        GlitchColorMode killerMode
    ) {
        String rendered = source.getString();

        if (Objects.equals(rendered, victimName)) {
            return Text.literal(markerForVictim(victimMode)).setStyle(source.getStyle());
        }

        if (Objects.equals(rendered, killerName)) {
            return Text.literal(markerForKiller(killerMode)).setStyle(source.getStyle());
        }

        TextContent content = source.getContent();
        MutableText result;

        if (content instanceof TranslatableTextContent translated) {
            Object[] args = Arrays.stream(translated.getArgs())
                .map(arg -> sanitizeArgument(
                    arg,
                    victimName,
                    killerName,
                    victimMode,
                    killerMode
                ))
                .toArray();

            result = Text.translatableWithFallback(
                translated.getKey(),
                translated.getFallback(),
                args
            );
        } else if (content instanceof PlainTextContent plain) {
            result = sanitizeLiteral(
                plain.string(),
                victimName,
                killerName,
                victimMode,
                killerMode,
                source.getStyle()
            );
        } else {
            result = source.copyContentOnly();
        }

        result.setStyle(source.getStyle());

        for (Text sibling : source.getSiblings()) {
            result.append(sanitizeText(
                sibling,
                victimName,
                killerName,
                victimMode,
                killerMode
            ));
        }

        return result;
    }

    private static MutableText sanitizeLiteral(
        String value,
        String victimName,
        String killerName,
        GlitchColorMode victimMode,
        GlitchColorMode killerMode,
        net.minecraft.text.Style style
    ) {
        if (value.isEmpty()) {
            return Text.literal("").setStyle(style);
        }

        MutableText result = Text.empty();
        int cursor = 0;

        while (cursor < value.length()) {
            int victimAt = value.indexOf(victimName, cursor);
            int killerAt = value.indexOf(killerName, cursor);

            int matchAt;
            String replacement;
            int matchedNameLength;

            if (victimAt < 0) {
                matchAt = killerAt;
                replacement = markerForKiller(killerMode);
                matchedNameLength = killerName.length();
            } else if (killerAt < 0) {
                matchAt = victimAt;
                replacement = markerForVictim(victimMode);
                matchedNameLength = victimName.length();
            } else if (victimAt <= killerAt) {
                matchAt = victimAt;
                replacement = markerForVictim(victimMode);
                matchedNameLength = victimName.length();
            } else {
                matchAt = killerAt;
                replacement = markerForKiller(killerMode);
                matchedNameLength = killerName.length();
            }

            if (matchAt < 0) {
                result.append(Text.literal(value.substring(cursor)).setStyle(style));
                break;
            }

            if (matchAt > cursor) {
                result.append(Text.literal(value.substring(cursor, matchAt)).setStyle(style));
            }

            result.append(Text.literal(replacement).setStyle(style));
            cursor = matchAt + matchedNameLength;
        }

        return result;
    }

    public static Text staticize(Text source) {
        return staticizeText(source);
    }

    private static Text staticizeText(Text source) {
        TextContent content = source.getContent();
        MutableText result;

        if (content instanceof TranslatableTextContent translated) {
            Object[] args = Arrays.stream(translated.getArgs())
                .map(GlitchTextSanitizer::staticizeArgument)
                .toArray();

            result = Text.translatableWithFallback(
                translated.getKey(),
                translated.getFallback(),
                args
            );
        } else if (content instanceof PlainTextContent plain) {
            result = staticizeLiteral(plain.string(), source.getStyle());
        } else {
            result = source.copyContentOnly();
        }

        result.setStyle(source.getStyle());

        for (Text sibling : source.getSiblings()) {
            result.append(staticizeText(sibling));
        }

        return result;
    }

    private static MutableText staticizeLiteral(String value, net.minecraft.text.Style style) {
        MutableText result = Text.empty();
        int cursor = 0;

        while (cursor < value.length()) {
            int markerAt = indexOfAnyMarker(value, cursor);

            if (markerAt < 0) {
                result.append(Text.literal(value.substring(cursor)).setStyle(style));
                break;
            }

            if (markerAt > cursor) {
                result.append(Text.literal(value.substring(cursor, markerAt)).setStyle(style));
            }

            GlitchColorMode mode = markerModeAt(value, markerAt);
            result.append(staticGlitchText(style, mode));
            cursor = markerAt + markerLengthAt(value, markerAt);
        }

        return result;
    }

    private static GlitchColorMode markerModeAt(String value, int markerAt) {
        if (value.startsWith(VICTIM_WHITE_MARKER, markerAt)
            || value.startsWith(KILLER_WHITE_MARKER, markerAt)) {
            return GlitchColorMode.WHITE;
        }
        return GlitchColorMode.COLORFUL;
    }

    private static int markerLengthAt(String value, int markerAt) {
        if (value.startsWith(VICTIM_WHITE_MARKER, markerAt)) {
            return VICTIM_WHITE_MARKER.length();
        }
        if (value.startsWith(KILLER_WHITE_MARKER, markerAt)) {
            return KILLER_WHITE_MARKER.length();
        }
        if (value.startsWith(KILLER_MARKER, markerAt)) {
            return KILLER_MARKER.length();
        }
        return VICTIM_MARKER.length();
    }

    private static Object staticizeArgument(Object argument) {
        if (argument instanceof Text text) {
            return staticizeText(text);
        }

        if (argument instanceof String string) {
            if (containsAnyMarker(string)) {
                return staticizeLiteral(string, net.minecraft.text.Style.EMPTY);
            }
        }

        return argument;
    }

    private static Text staticGlitchText(
        net.minecraft.text.Style style,
        GlitchColorMode mode
    ) {
        GlitchFrameGenerator.Frame frame = GlitchFrameGenerator.next(mode);

        MutableText result = Text.empty().setStyle(style);
        int[] codePoints = frame.text().codePoints().toArray();

        for (int i = 0; i < codePoints.length; i++) {
            result.append(
                Text.literal(new String(Character.toChars(codePoints[i])))
                    .setStyle(style.withColor(frame.colors()[i] & 0xFFFFFF))
            );
        }

        return result;
    }

    private static Object sanitizeArgument(
        Object argument,
        String victimName,
        String killerName,
        GlitchColorMode victimMode,
        GlitchColorMode killerMode
    ) {
        if (argument instanceof Text text) {
            return sanitizeText(
                text,
                victimName,
                killerName,
                victimMode,
                killerMode
            );
        }

        if (argument instanceof String string) {
            if (Objects.equals(string, victimName)) {
                return markerForVictim(victimMode);
            }

            if (Objects.equals(string, killerName)) {
                return markerForKiller(killerMode);
            }
        }

        return argument;
    }

    private static String markerForVictim(GlitchColorMode mode) {
        return mode == GlitchColorMode.WHITE
            ? VICTIM_WHITE_MARKER
            : VICTIM_MARKER;
    }

    private static String markerForKiller(GlitchColorMode mode) {
        return mode == GlitchColorMode.WHITE
            ? KILLER_WHITE_MARKER
            : KILLER_MARKER;
    }

    private static boolean containsAnyMarker(String value) {
        return value.contains(VICTIM_MARKER)
            || value.contains(KILLER_MARKER)
            || value.contains(VICTIM_WHITE_MARKER)
            || value.contains(KILLER_WHITE_MARKER);
    }

    private static int indexOfAnyMarker(String value, int cursor) {
        int victimAt = value.indexOf(VICTIM_MARKER, cursor);
        int killerAt = value.indexOf(KILLER_MARKER, cursor);
        int victimWhiteAt = value.indexOf(VICTIM_WHITE_MARKER, cursor);
        int killerWhiteAt = value.indexOf(KILLER_WHITE_MARKER, cursor);

        int result = Integer.MAX_VALUE;
        if (victimAt >= 0) result = Math.min(result, victimAt);
        if (killerAt >= 0) result = Math.min(result, killerAt);
        if (victimWhiteAt >= 0) result = Math.min(result, victimWhiteAt);
        if (killerWhiteAt >= 0) result = Math.min(result, killerWhiteAt);

        return result == Integer.MAX_VALUE ? -1 : result;
    }
}
