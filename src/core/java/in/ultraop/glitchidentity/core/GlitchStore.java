package in.ultraop.glitchidentity.core;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class GlitchStore {
    private final Set<UUID> enabled = new LinkedHashSet<>();
    public boolean add(UUID id) { return enabled.add(id); }
    public boolean remove(UUID id) { return enabled.remove(id); }
    public boolean contains(UUID id) { return enabled.contains(id); }
    public Set<UUID> all() { return Set.copyOf(enabled); }
    public void clear() { enabled.clear(); }
    public void load(Iterable<UUID> ids) { enabled.clear(); ids.forEach(enabled::add); }
}
