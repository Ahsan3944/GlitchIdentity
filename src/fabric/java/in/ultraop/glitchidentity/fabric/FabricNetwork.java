package in.ultraop.glitchidentity.fabric;

import net.minecraft.entity.Entity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class FabricNetwork {
    private FabricNetwork() {}

    public static Text createSafeDeathMessage(
        ServerPlayerEntity victim,
        ServerPlayerEntity killer,
        boolean glitchVictim,
        boolean glitchKiller
    ) {
        Text deathMessage = victim.getDamageTracker().getDeathMessage();

        Text sanitized = deathMessage;

        if (glitchVictim) {
            sanitized = GlitchTextSanitizer.sanitize(
                sanitized,
                victim.getName().getString(),
                "\u0000never-killer\u0000"
            );
            sanitized = GlitchTextSanitizer.sanitize(
                sanitized,
                victim.getDisplayName().getString(),
                "\u0000never-killer-display\u0000"
            );
        }

        if (glitchKiller && killer != null) {
            sanitized = GlitchTextSanitizer.sanitize(
                sanitized,
                "\u0000never-victim\u0000",
                killer.getName().getString()
            );
            sanitized = GlitchTextSanitizer.sanitize(
                sanitized,
                "\u0000never-victim-display\u0000",
                killer.getDisplayName().getString()
            );
        }

        return sanitized;
    }

    public static ServerPlayerEntity resolvePlayerAttacker(DamageSource damageSource) {
        Entity attacker = damageSource.getAttacker();
        if (attacker instanceof ServerPlayerEntity player) {
            return player;
        }

        Entity source = damageSource.getSource();
        if (source instanceof ServerPlayerEntity player) {
            return player;
        }

        if (source instanceof Ownable ownable) {
            Entity owner = ownable.getOwner();
            if (owner instanceof ServerPlayerEntity player) {
                return player;
            }
        }

        return null;
    }
}
