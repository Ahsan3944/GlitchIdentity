package in.ultraop.glitchidentity.core;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GlitchStore {
    private final Map<UUID, GlitchColorMode> enabled = new LinkedHashMap<>();

    public boolean add(UUID id) {
        if (enabled.containsKey(id)) {
            return false;
        }
        enabled.put(id, GlitchColorMode.COLORFUL);
        return true;
    }

    public boolean add(UUID id, GlitchColorMode mode) {
        GlitchColorMode selectedMode = mode == null ? GlitchColorMode.COLORFUL : mode;
        GlitchColorMode previous = enabled.put(id, selectedMode);
        return previous != selectedMode;
    }

    public boolean remove(UUID id) {
        return enabled.remove(id) != null;
    }

    public boolean contains(UUID id) {
        return enabled.containsKey(id);
    }

    public GlitchColorMode modeOf(UUID id) {
        return enabled.getOrDefault(id, GlitchColorMode.COLORFUL);
    }

    public Set<UUID> all() {
        return new LinkedHashSet<>(enabled.keySet());
    }

    public void clear() {
        enabled.clear();
    }

    public void load(Iterable<UUID> ids) {
        enabled.clear();
        ids.forEach(this::add);
    }
}
