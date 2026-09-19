package in.ultraop.glitchidentity.fabric;

import in.ultraop.glitchidentity.core.GlitchFrameGenerator;
import net.minecraft.text.OrderedText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AnimatedGlitchText implements Text {
    private final Text template;
    private final boolean glitchVictim;
    private final boolean glitchKiller;
    private final GlitchSegment victimGlitch = new GlitchSegment();
    private final GlitchSegment killerGlitch = new GlitchSegment();

    private AnimatedGlitchText(Text template, boolean glitchVictim, boolean glitchKiller) {
        this.template = template;
        this.glitchVictim = glitchVictim;
        this.glitchKiller = glitchKiller;
    }

    public static AnimatedGlitchText death(Text template, boolean glitchVictim, boolean glitchKiller) {
        return new AnimatedGlitchText(template, glitchVictim, glitchKiller);
    }

    @Override
    public PlainTextContent getContent() {
        StringBuilder root = new StringBuilder();
        template.getContent().visit(value -> {
            root.append(value);
            return Optional.empty();
        });
        return (PlainTextContent) Text.literal(renderString(root.toString())).getContent();
    }

    @Override
    public Style getStyle() {
        return template.getStyle();
    }

    @Override
    public List<Text> getSiblings() {
        return template.getSiblings();
    }

    @Override
    public String getString() {
        return renderString(template.getString());
    }

    @Override
    public OrderedText asOrderedText() {
        List<OrderedText> parts = new ArrayList<>();
        appendRendered(parts, template);
        return parts.isEmpty() ? OrderedText.EMPTY : OrderedText.concat(parts);
    }

    private void appendRendered(List<OrderedText> parts, Text text) {
        appendRoot(parts, text);
        for (Text sibling : text.getSiblings()) {
            if (containsMarker(sibling.getString())) {
                parts.add(AnimatedGlitchText.death(
                    sibling,
                    sibling.getString().contains(GlitchTextSanitizer.VICTIM_MARKER),
                    sibling.getString().contains(GlitchTextSanitizer.KILLER_MARKER)
                ).asOrderedText());
            } else {
                parts.add(sibling.asOrderedText());
            }
        }
    }

    private void appendRoot(List<OrderedText> parts, Text text) {
        String full = text.getString();
        int cursor = 0;

        while (cursor < full.length()) {
            int victimAt = glitchVictim ? full.indexOf(GlitchTextSanitizer.VICTIM_MARKER, cursor) : -1;
            int killerAt = glitchKiller ? full.indexOf(GlitchTextSanitizer.KILLER_MARKER, cursor) : -1;

            int markerAt;
            GlitchSegment segment;

            if (victimAt < 0) {
                markerAt = killerAt;
                segment = killerGlitch;
            } else if (killerAt < 0) {
                markerAt = victimAt;
                segment = victimGlitch;
            } else if (victimAt < killerAt) {
                markerAt = victimAt;
                segment = victimGlitch;
            } else {
                markerAt = killerAt;
                segment = killerGlitch;
            }

            if (markerAt < 0) {
                parts.add(Text.literal(full.substring(cursor))
                    .setStyle(text.getStyle())
                    .asOrderedText());
                break;
            }

            if (markerAt > cursor) {
                parts.add(Text.literal(full.substring(cursor, markerAt))
                    .setStyle(text.getStyle())
                    .asOrderedText());
            }

            parts.add(segment.asOrderedText(text.getStyle()));

            cursor = markerAt + (
                segment == victimGlitch
                    ? GlitchTextSanitizer.VICTIM_MARKER.length()
                    : GlitchTextSanitizer.KILLER_MARKER.length()
            );
        }
    }

    private static boolean containsMarker(String value) {
        return value.contains(GlitchTextSanitizer.VICTIM_MARKER)
            || value.contains(GlitchTextSanitizer.KILLER_MARKER);
    }

    private String renderString(String value) {
        return value
            .replace(
                GlitchTextSanitizer.VICTIM_MARKER,
                glitchVictim ? victimGlitch.snapshot().text() : ""
            )
            .replace(
                GlitchTextSanitizer.KILLER_MARKER,
                glitchKiller ? killerGlitch.snapshot().text() : ""
            );
    }

    private static final class GlitchSegment {
        private static final long FRAME_HOLD_NANOS = 30_000_000L;
        private GlitchFrameGenerator.Frame frame = GlitchFrameGenerator.next();
        private long frameAt = System.nanoTime();

        private synchronized GlitchFrameGenerator.Frame snapshot() {
            long now = System.nanoTime();
            if (now - frameAt >= FRAME_HOLD_NANOS) {
                frame = GlitchFrameGenerator.next();
                frameAt = now;
            }
            return frame;
        }

        private OrderedText asOrderedText(Style baseStyle) {
            return visitor -> {
                GlitchFrameGenerator.Frame current = snapshot();
                int[] codePoints = current.text().codePoints().toArray();

                for (int i = 0; i < codePoints.length; i++) {
                    if (!visitor.accept(
                        i,
                        baseStyle.withColor(current.colors()[i] & 0xFFFFFF),
                        codePoints[i]
                    )) {
                        return false;
                    }
                }
                return true;
            };
        }
    }
}