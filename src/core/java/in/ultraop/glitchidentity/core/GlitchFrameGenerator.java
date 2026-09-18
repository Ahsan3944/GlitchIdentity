package in.ultraop.glitchidentity.core;

import java.util.concurrent.ThreadLocalRandom;

public final class GlitchFrameGenerator {
    private static final char[] POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789#@$%&*+=?!_".toCharArray();
    private static final int[] COLORS = {
        0x55FFFF, 0xFF55FF, 0xFFFF55, 0x55FF55, 0xFF5555, 0x5555FF, 0xFFFFFF
    };

    private GlitchFrameGenerator() {}

    public static Frame next(int length) {
        length = Math.max(5, Math.min(7, length));
        var random = ThreadLocalRandom.current();
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
