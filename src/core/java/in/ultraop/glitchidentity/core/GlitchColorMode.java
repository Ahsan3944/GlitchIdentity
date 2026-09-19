package in.ultraop.glitchidentity.core;

public enum GlitchColorMode {
    COLORFUL,
    WHITE;

    public static GlitchColorMode fromArgument(String value) {
        if (value == null) {
            return null;
        }

        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "colorful" -> COLORFUL;
            case "white" -> WHITE;
            default -> null;
        };
    }
}
