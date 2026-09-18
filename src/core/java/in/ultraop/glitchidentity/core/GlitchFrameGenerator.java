package in.ultraop.glitchidentity.core;

import java.util.concurrent.ThreadLocalRandom;

public final class GlitchFrameGenerator {
    // Hacker/corrupted identity palette: deliberately avoids a plain, uniform look.
    private static final char[] POOL =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789#@$%&*+=?!_<>[]{}\\/|".toCharArray();

    private static final int[] COLORS = {
        0xFF1744, 0xFFEA00, 0x00E5FF, 0xD500F9,
        0x76FF03, 0xFF6D00, 0x651FFF, 0xFFFFFF
    };

    private GlitchFrameGenerator() {}

    /**
     * Every frame gets a fresh random length from 5 through 7.
     * Each slot independently gets a fresh character and color.
     */
    public static Frame next() {
        var random = ThreadLocalRandom.current();
        int length = 5 + random.nextInt(3); // 5, 6, or 7
        StringBuilder text = new StringBuilder(length);
        int[] colors = new int[length];

        for (int i = 0; i < length; i++) {
            text.append(POOL[random.nextInt(POOL.length)]);
            colors[i] = COLORS[random.nextInt(COLORS.length)];
        }
        return new Frame(text.toString(), colors);
    }

    public record Frame(String text, int[] colors) {}
}
