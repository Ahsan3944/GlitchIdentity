package in.ultraop.glitchidentity.fabric;

import net.minecraft.text.MutableText;
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
        } else {
            result = source.copyContentOnly();
        }

        result.setStyle(source.getStyle());
        for (Text sibling : source.getSiblings()) {
            result.append(sanitizeText(sibling, victimName, killerName));
        }

        return result;
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
