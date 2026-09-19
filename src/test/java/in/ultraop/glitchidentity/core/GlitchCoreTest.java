package in.ultraop.glitchidentity.core;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GlitchCoreTest {
    @Test
    void generatedFramesHaveFiveToSevenCharactersAndMatchingColors() {
        for (int i = 0; i < 500; i++) {
            GlitchFrameGenerator.Frame frame = GlitchFrameGenerator.next();
            int[] codePoints = frame.text().codePoints().toArray();

            assertTrue(codePoints.length >= 5 && codePoints.length <= 7);
            assertEquals(codePoints.length, frame.colors().length);
            for (int color : frame.colors()) {
                assertEquals(0xFF, (color >>> 24) & 0xFF);
            }
        }
    }

    @Test
    void generatedFramesUseTheConfiguredCharacterPool() {
        String sample = GlitchFrameGenerator.next().text();
        String allowed =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "abcdefghijklmnopqrstuvwxyz" +
            "0123456789" +
            "#@$%&*+=?!_<>[]{}\\/|~^" +
            "¢£¤¥₠₡₢₣₤₥₦₧₨₩₪₫€₭₮₯₰₱₲₳₴₵₸₹₺₼₽₾₿" +
            "ÆæŒœØøÐðÞþŁłĐđŦŧƵƶƔɎʤʣʕʘ" +
            "ΔδΘθΛλΞξΠπΣσΦφΨψΩω" +
            "αβγδεζηικμνξοπρστφχψω" +
            "←↑→↓↔↕↖↗↘↙⇐⇒⇔⇑⇓⇕⇖⇗⇘⇙⇞⇟" +
            "∂∇√∞∑∏∫∮∴∵≈≠≤≥±×÷∩∪∧∨⊕⊗⊙⊥" +
            "⩓⩠⪔⪕⪖⪗⪙⪚⪛⪜⪝" +
            "ⅧⅨⅩⅪⅫↂↈ∳∲" +
            "§¶†‡•°※⁂★☆✦✧✶✪❖✸✻" +
            "◆◇◈●○■□▲△▼▽" +
            "─│┌┐└┘├┤┬┴┼═║╔╗╚╝╠╣╦╩╬▀▄█░▒▓";

        sample.codePoints().forEach(cp ->
            assertTrue(allowed.indexOf(cp) >= 0, "Unexpected code point: " + cp)
        );
    }

    @Test
    void generatedFramesUsuallyChange() {
        Set<String> frames = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            frames.add(GlitchFrameGenerator.next().text());
        }
        assertTrue(frames.size() >= 90, "Too many duplicate frames generated");
    }

    @Test
    void messageFingerprintIsStableAndChangesWithMessage() {
        String first = GlitchMessageKey.of("kokoro was slain by gsgsfee");
        String second = GlitchMessageKey.of("kokoro was slain by gsgsfee");
        String different = GlitchMessageKey.of("kokoro left the game");

        assertEquals(first, second);
        assertNotEquals(first, different);
        assertEquals(64, first.length());
    }

    @Test
    void storeIsIdempotentAndRemovesCorrectly() {
        GlitchStore store = new GlitchStore();
        UUID id = UUID.randomUUID();

        assertTrue(store.add(id));
        assertFalse(store.add(id));
        assertTrue(store.contains(id));
        assertTrue(store.remove(id));
        assertFalse(store.contains(id));
        assertFalse(store.remove(id));
    }
}
