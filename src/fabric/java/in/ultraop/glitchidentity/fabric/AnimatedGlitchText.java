package in.ultraop.glitchidentity.fabric;

import in.ultraop.glitchidentity.core.GlitchFrameGenerator;
import net.minecraft.text.OrderedText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.List;

public final class AnimatedGlitchText implements Text {
    private final String victim;
    private final String killer;
    private final boolean glitchVictim;
    private final boolean glitchKiller;

    private final GlitchSegment victimGlitch = new GlitchSegment();
    private final GlitchSegment killerGlitch = new GlitchSegment();

    private AnimatedGlitchText(
        String victim,
        String killer,
        boolean glitchVictim,
        boolean glitchKiller
    ) {
        this.victim = victim;
        this.killer = killer;
        this.glitchVictim = glitchVictim;
        this.glitchKiller = glitchKiller;
    }

    public static AnimatedGlitchText death(
        String victim,
        String killer,
        boolean glitchVictim,
        boolean glitchKiller
    ) {
        return new AnimatedGlitchText(victim, killer, glitchVictim, glitchKiller);
    }

    @Override
    public PlainTextContent getContent() {
        return PlainTextContent.EMPTY;
    }

    @Override
    public Style getStyle() {
        return Style.EMPTY;
    }

    @Override
    public List<Text> getSiblings() {
        return List.of();
    }

    @Override
    public String getString() {
        String victimText = glitchVictim ? victimGlitch.snapshot().text() : victim;
        if (killer == null || killer.isEmpty()) {
            return victimText + " died";
        }

        String killerText = glitchKiller ? killerGlitch.snapshot().text() : killer;
        return victimText + " was slain by " + killerText;
    }

    @Override
    public OrderedText asOrderedText() {
        OrderedText victimText = glitchVictim
            ? victimGlitch.asOrderedText()
            : Text.literal(victim).asOrderedText();

        OrderedText separator = Text.literal(
            killer == null || killer.isEmpty() ? " died" : " was slain by "
        ).asOrderedText();

        if (killer == null || killer.isEmpty()) {
            return OrderedText.concat(victimText, separator);
        }

        OrderedText killerText = glitchKiller
            ? killerGlitch.asOrderedText()
            : Text.literal(killer).asOrderedText();

        return OrderedText.concat(victimText, separator, killerText);
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
                        Style.EMPTY.withColor(current.colors()[i]),
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
