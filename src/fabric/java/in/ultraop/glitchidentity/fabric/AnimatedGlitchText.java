package in.ultraop.glitchidentity.fabric;

import in.ultraop.glitchidentity.core.GlitchFrameGenerator;
import net.minecraft.text.OrderedText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

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

    public static AnimatedGlitchText death(
        Text template,
        boolean glitchVictim,
        boolean glitchKiller
    ) {
        return new AnimatedGlitchText(template, glitchVictim, glitchKiller);
    }

    @Override
    public PlainTextContent getContent() {
        return (PlainTextContent) Text.literal(
            renderString(template.getString())
        ).getContent();
    }

    @Override
    public Style getStyle() {
        return template.getStyle();
    }

    @Override
    public List<Text> getSiblings() {
        return List.of();
    }

    @Override
    public String getString() {
        return renderString(template.getString());
    }

    @Override
    public OrderedText asOrderedText() {
        String value = template.getString();
        List<OrderedText> parts = new ArrayList<>();
        int cursor = 0;

        while (cursor < value.length()) {
            int victimAt = glitchVictim ? value.indexOf(GlitchTextSanitizer.VICTIM_MARKER, cursor) : -1;
            int killerAt = glitchKiller ? value.indexOf(GlitchTextSanitizer.KILLER_MARKER, cursor) : -1;

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
                parts.add(Text.literal(value.substring(cursor)).asOrderedText());
                break;
            }

            if (markerAt > cursor) {
                parts.add(Text.literal(value.substring(cursor, markerAt)).asOrderedText());
            }

            parts.add(segment.asOrderedText());

            if (segment == victimGlitch) {
                cursor = markerAt + GlitchTextSanitizer.VICTIM_MARKER.length();
            } else {
                cursor = markerAt + GlitchTextSanitizer.KILLER_MARKER.length();
            }
        }

        return parts.isEmpty() ? OrderedText.EMPTY : OrderedText.concat(parts);
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

        private OrderedText asOrderedText() {
            return visitor -> {
                GlitchFrameGenerator.Frame current = snapshot();
                int[] codePoints = current.text().codePoints().toArray();

                for (int i = 0; i < codePoints.length; i++) {
                    if (!visitor.accept(
                        i,
                        Style.EMPTY.withColor(current.colors()[i] & 0xFFFFFF),
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
