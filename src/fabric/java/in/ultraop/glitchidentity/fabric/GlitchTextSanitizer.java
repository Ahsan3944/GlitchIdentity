package in.ultraop.glitchidentity.fabric;

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

    private GlitchTextSanitizer() {}

    public static Text sanitize(Text source, String victimName, String killerName) {
        return sanitizeText(source, victimName, killerName);
    }

    private static Text sanitizeText(Text source, String victimName, String killerName) {
        String rendered = source.getString();

        if (Objects.equals(rendered, victimName)) {
            return Text.literal(VICTIM_MARKER).setStyle(source.getStyle());
        }

        if (Objects.equals(rendered, killerName)) {
            return Text.literal(KILLER_MARKER).setStyle(source.getStyle());
        }

        TextContent content = source.getContent();
        MutableText result;

        if (content instanceof TranslatableTextContent translated) {
            Object[] args = Arrays.stream(translated.getArgs())
                .map(arg -> sanitizeArgument(arg, victimName, killerName))
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
                source.getStyle()
            );
        } else {
            result = source.copyContentOnly();
        }

        result.setStyle(source.getStyle());

        for (Text sibling : source.getSiblings()) {
            result.append(sanitizeText(sibling, victimName, killerName));
        }

        return result;
    }

    private static MutableText sanitizeLiteral(
        String value,
        String victimName,
        String killerName,
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

            if (victimAt < 0) {
                matchAt = killerAt;
                replacement = KILLER_MARKER;
            } else if (killerAt < 0) {
                matchAt = victimAt;
                replacement = VICTIM_MARKER;
            } else if (victimAt <= killerAt) {
                matchAt = victimAt;
                replacement = VICTIM_MARKER;
            } else {
                matchAt = killerAt;
                replacement = KILLER_MARKER;
            }

            if (matchAt < 0) {
                result.append(Text.literal(value.substring(cursor)).setStyle(style));
                break;
            }

            if (matchAt > cursor) {
                result.append(Text.literal(value.substring(cursor, matchAt)).setStyle(style));
            }

            result.append(Text.literal(replacement).setStyle(style));
            cursor = matchAt + replacementNameLength(
                replacement.equals(VICTIM_MARKER) ? victimName : killerName
            );
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
            int victimAt = value.indexOf(VICTIM_MARKER, cursor);
            int killerAt = value.indexOf(KILLER_MARKER, cursor);

            int markerAt;
            String marker;

            if (victimAt < 0) {
                markerAt = killerAt;
                marker = KILLER_MARKER;
            } else if (killerAt < 0) {
                markerAt = victimAt;
                marker = VICTIM_MARKER;
            } else if (victimAt <= killerAt) {
                markerAt = victimAt;
                marker = VICTIM_MARKER;
            } else {
                markerAt = killerAt;
                marker = KILLER_MARKER;
            }

            if (markerAt < 0) {
                result.append(Text.literal(value.substring(cursor)).setStyle(style));
                break;
            }

            if (markerAt > cursor) {
                result.append(Text.literal(value.substring(cursor, markerAt)).setStyle(style));
            }

            result.append(staticGlitchText(style));
            cursor = markerAt + marker.length();
        }

        return result;
    }

    private static Object staticizeArgument(Object argument) {
        if (argument instanceof Text text) {
            return staticizeText(text);
        }

        if (argument instanceof String string) {
            if (string.contains(VICTIM_MARKER) || string.contains(KILLER_MARKER)) {
                return staticizeLiteral(string, net.minecraft.text.Style.EMPTY);
            }
        }

        return argument;
    }

    private static Text staticGlitchText(net.minecraft.text.Style style) {
        GlitchFrameGenerator.Frame frame = GlitchFrameGenerator.next();
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

    private static int replacementNameLength(String name) {
        return name.length();
    }

    private static Object sanitizeArgument(
        Object argument,
        String victimName,
        String killerName
    ) {
        if (argument instanceof Text text) {
            return sanitizeText(text, victimName, killerName);
        }

        if (argument instanceof String string) {
            if (Objects.equals(string, victimName)) {
                return VICTIM_MARKER;
            }

            if (Objects.equals(string, killerName)) {
                return KILLER_MARKER;
            }
        }

        return argument;
    }
}
