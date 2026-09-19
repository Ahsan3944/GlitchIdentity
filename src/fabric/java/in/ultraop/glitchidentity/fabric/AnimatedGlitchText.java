package in.ultraop.glitchidentity.fabric;

import in.ultraop.glitchidentity.core.GlitchColorMode;
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
    private final GlitchSegment victimGlitch;
    private final GlitchSegment killerGlitch;

    private AnimatedGlitchText(
        Text template,
        boolean glitchVictim,
        boolean glitchKiller,
        GlitchColorMode victimMode,
        GlitchColorMode killerMode
    ) {
        this.template = template;
        this.glitchVictim = glitchVictim;
        this.glitchKiller = glitchKiller;
        this.victimGlitch = new GlitchSegment(victimMode);
        this.killerGlitch = new GlitchSegment(killerMode);
    }

    public static AnimatedGlitchText death(
        Text template,
        boolean glitchVictim,
        boolean glitchKiller
    ) {
        return death(
            template,
            glitchVictim,
            glitchKiller,
            GlitchColorMode.COLORFUL,
            GlitchColorMode.COLORFUL
        );
    }

    public static AnimatedGlitchText death(
        Text template,
        boolean glitchVictim,
        boolean glitchKiller,
        GlitchColorMode victimMode,
        GlitchColorMode killerMode
    ) {
        return new AnimatedGlitchText(
            template,
            glitchVictim,
            glitchKiller,
            victimMode,
            killerMode
        );
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
                    containsVictimMarker(sibling.getString()),
                    containsKillerMarker(sibling.getString()),
                    containsWhiteVictimMarker(sibling.getString())
                        ? GlitchColorMode.WHITE
                        : GlitchColorMode.COLORFUL,
                    containsWhiteKillerMarker(sibling.getString())
                        ? GlitchColorMode.WHITE
                        : GlitchColorMode.COLORFUL
                ).asOrderedText());
            } else {
                parts.add(sibling.asOrderedText());
            }
        }
    }

    private void appendRoot(List<OrderedText> parts, Text text) {
        String full = rootContentString(text);
        int cursor = 0;

        while (cursor < full.length()) {
            int victimAt = glitchVictim ? indexOfVictimMarker(full, cursor) : -1;
            int killerAt = glitchKiller ? indexOfKillerMarker(full, cursor) : -1;

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

            cursor = markerAt + GlitchTextSanitizer.VICTIM_MARKER.length();
        }
    }

    private static String rootContentString(Text text) {
        StringBuilder value = new StringBuilder();
        text.getContent().visit(part -> {
            value.append(part);
            return Optional.empty();
        });
        return value.toString();
    }

    private static boolean containsMarker(String value) {
        return containsVictimMarker(value) || containsKillerMarker(value);
    }

    private static boolean containsVictimMarker(String value) {
        return value.contains(GlitchTextSanitizer.VICTIM_MARKER)
            || value.contains(GlitchTextSanitizer.VICTIM_WHITE_MARKER);
    }

    private static boolean containsKillerMarker(String value) {
        return value.contains(GlitchTextSanitizer.KILLER_MARKER)
            || value.contains(GlitchTextSanitizer.KILLER_WHITE_MARKER);
    }

    private static boolean containsWhiteVictimMarker(String value) {
        return value.contains(GlitchTextSanitizer.VICTIM_WHITE_MARKER);
    }

    private static boolean containsWhiteKillerMarker(String value) {
        return value.contains(GlitchTextSanitizer.KILLER_WHITE_MARKER);
    }

    private static int indexOfVictimMarker(String value, int cursor) {
        int colorfulAt = value.indexOf(GlitchTextSanitizer.VICTIM_MARKER, cursor);
        int whiteAt = value.indexOf(GlitchTextSanitizer.VICTIM_WHITE_MARKER, cursor);

        if (colorfulAt < 0) return whiteAt;
        if (whiteAt < 0) return colorfulAt;
        return Math.min(colorfulAt, whiteAt);
    }

    private static int indexOfKillerMarker(String value, int cursor) {
        int colorfulAt = value.indexOf(GlitchTextSanitizer.KILLER_MARKER, cursor);
        int whiteAt = value.indexOf(GlitchTextSanitizer.KILLER_WHITE_MARKER, cursor);

        if (colorfulAt < 0) return whiteAt;
        if (whiteAt < 0) return colorfulAt;
        return Math.min(colorfulAt, whiteAt);
    }

    private String renderString(String value) {
        return value
            .replace(
                GlitchTextSanitizer.VICTIM_MARKER,
                glitchVictim ? victimGlitch.snapshot().text() : ""
            )
            .replace(
                GlitchTextSanitizer.VICTIM_WHITE_MARKER,
                glitchVictim ? victimGlitch.snapshot().text() : ""
            )
            .replace(
                GlitchTextSanitizer.KILLER_MARKER,
                glitchKiller ? killerGlitch.snapshot().text() : ""
            )
            .replace(
                GlitchTextSanitizer.KILLER_WHITE_MARKER,
                glitchKiller ? killerGlitch.snapshot().text() : ""
            );
    }

    private static final class GlitchSegment {
        private static final long FRAME_HOLD_NANOS = 30_000_000L;
        private final GlitchColorMode mode;
        private GlitchFrameGenerator.Frame frame;
        private long frameAt;

        private GlitchSegment(GlitchColorMode mode) {
            this.mode = mode == null ? GlitchColorMode.COLORFUL : mode;
            this.frame = GlitchFrameGenerator.next(this.mode);
            this.frameAt = System.nanoTime();
        }

        private synchronized GlitchFrameGenerator.Frame snapshot() {
            long now = System.nanoTime();
            if (now - frameAt >= FRAME_HOLD_NANOS) {
                frame = GlitchFrameGenerator.next(mode);
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
