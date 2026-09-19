package in.ultraop.glitchidentity.core;

public final class GlitchMessages {
    private GlitchMessages() {}

    public static final String PREFIX = "§8[§dGlitch§8] §r";
    public static final String VERSION = "1.0.0";

    public static final String HELP = String.join("\n",
        "§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        "§d§l Glitch Identity §7v" + VERSION,
        "§7Animated / corrupted player identities",
        "",
        "§d§lCommands",
        "§f/glitch add <player> <color> §8- §7Enable glitch; color: colorful or white",
        "§f/glitch add @ <color> §8- §7Enable glitch for all online players",
        "§f/glitch remove <player> §8- §7Disable glitch for a player",
        "§f/glitch remove @ §8- §7Disable glitch for everyone",
        "§f/glitch list §8- §7Show configured glitch players and colors",
        "§f/glitch reload §8- §7Reload GlitchIdentity configuration",
        "§f/glitch version §8- §7Show plugin version",
        "§f/glitch help §8- §7Show this help",
        "",
        "§d§lColors",
        "§7§fcolorful §8- §7Existing randomized glitch colors",
        "§7§fwhite §8- §7White glitch text",
        "",
        "§d§lSuggestions",
        "§7Add: §fonly online players without glitch + @, then colorful or white",
        "§7Remove: §fonline configured players + @",
        "",
        "§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        "§7Created by §d§lUltra OP"
    );

    public static final String VERSION_MESSAGE =
        PREFIX + "§dGlitch Identity §7version §f" + VERSION
            + " §8• §7Created by §dUltra OP";
}
