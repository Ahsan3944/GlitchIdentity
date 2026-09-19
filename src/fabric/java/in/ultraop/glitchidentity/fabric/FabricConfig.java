package in.ultraop.glitchidentity.fabric;

import in.ultraop.glitchidentity.core.GlitchColorMode;
import in.ultraop.glitchidentity.core.GlitchStore;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public final class FabricConfig {
    private static final Path FILE = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("glitchidentity")
        .resolve("players.txt");

    private FabricConfig() {}

    public static void load(GlitchStore store) {
        store.clear();
        if (!Files.exists(FILE)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                String value = line.trim();
                if (value.isEmpty() || value.startsWith("#")) {
                    continue;
                }

                try {
                    String[] parts = value.split("\\|", 2);
                    UUID id = UUID.fromString(parts[0].trim());
                    GlitchColorMode mode = parts.length == 1
                        ? GlitchColorMode.COLORFUL
                        : GlitchColorMode.fromArgument(parts[1].trim());

                    if (mode == null) {
                        System.err.println(
                            "[GlitchIdentity] Ignoring malformed color mode in " + FILE + ": " + value
                        );
                        continue;
                    }

                    store.add(id, mode);
                } catch (IllegalArgumentException ignored) {
                    System.err.println("[GlitchIdentity] Ignoring malformed UUID in " + FILE + ": " + value);
                }
            }
        } catch (IOException e) {
            System.err.println("[GlitchIdentity] Could not read " + FILE + ": " + e.getMessage());
        }
    }

    public static void save(GlitchStore store) {
        try {
            Files.createDirectories(FILE.getParent());

            Path temp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            StringBuilder data = new StringBuilder();
            for (UUID id : store.all()) {
                data.append(id)
                    .append("|")
                    .append(store.modeOf(id).name())
                    .append(System.lineSeparator());
            }

            Files.writeString(temp, data.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(
                    temp,
                    FILE,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                );
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("[GlitchIdentity] Could not save " + FILE + ": " + e.getMessage());
        }
    }
}
