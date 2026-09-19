package in.ultraop.glitchidentity.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class GlitchMessageKey {
    private GlitchMessageKey() {}

    public static String of(String message) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(message.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(Character.forDigit((value >>> 4) & 0xF, 16));
                result.append(Character.forDigit(value & 0xF, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
