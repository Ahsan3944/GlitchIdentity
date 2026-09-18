package in.ultraop.glitchidentity.core;

import org.junit.jupiter.api.Test;

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
