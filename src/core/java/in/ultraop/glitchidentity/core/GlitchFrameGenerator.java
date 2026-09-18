package in.ultraop.glitchidentity.core;

import java.util.concurrent.ThreadLocalRandom;

public final class GlitchFrameGenerator {
    /*
     * Deliberately mixed Unicode/ASCII glitch pool.
     * Minecraft clients do not render every Unicode glyph in every font, so this
     * pool focuses on BMP characters commonly available to Java/Minecraft fonts.
     */
    private static final int[] POOL = (
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
        "ⅧⅨⅩⅪⅫ" +
        "ↂↈ∳∲" +
        "§¶†‡•°※⁂" +
        "★☆✦✧✶✪❖✸✻" +
        "◆◇◈●○■□▲△▼▽" +
        "─│┌┐└┘├┤┬┴┼═║╔╗╚╝╠╣╦╩╬" +
        "▀▄█░▒▓"
    ).codePoints().toArray();

    private static final int[] COLORS = {
        0xFFFF1744, 0xFFFFEA00, 0xFF00E5FF, 0xFFD500F9,
        0xFF76FF03, 0xFFFF6D00, 0xFF651FFF, 0xFFFFFFFF
    };

    private static String lastFrame = "";

    private GlitchFrameGenerator() {}

    /**
     * Creates a fresh 5-7 character identity every frame.
     * Characters and colors are independently randomized.
     * Consecutive identical frames are rejected to prevent visible repetition.
     */
    public static synchronized Frame next() {
        var random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < 12; attempt++) {
            int length = 5 + random.nextInt(3);
            StringBuilder text = new StringBuilder(length);
            int[] colors = new int[length];

            int previousCodePoint = -1;
            for (int i = 0; i < length; i++) {
                int codePoint;
                do {
                    codePoint = POOL[random.nextInt(POOL.length)];
                } while (codePoint == previousCodePoint && POOL.length > 1);

                previousCodePoint = codePoint;
                text.appendCodePoint(codePoint);
                colors[i] = COLORS[random.nextInt(COLORS.length)];
            }

            String value = text.toString();
            if (!value.equals(lastFrame)) {
                lastFrame = value;
                return new Frame(value, colors);
            }
        }

        // The pool is large enough that this is only a defensive fallback.
        int length = 5 + random.nextInt(3);
        StringBuilder text = new StringBuilder(length);
        int[] colors = new int[length];
        for (int i = 0; i < length; i++) {
            int codePoint = POOL[random.nextInt(POOL.length)];
            text.appendCodePoint(codePoint);
            colors[i] = COLORS[random.nextInt(COLORS.length)];
        }
        lastFrame = text.toString();
        return new Frame(lastFrame, colors);
    }

    public record Frame(String text, int[] colors) {}
}
