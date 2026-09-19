package in.ultraop.glitchidentity.fabric;

import in.ultraop.glitchidentity.core.GlitchMessageKey;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public final class FabricNetwork {
    private FabricNetwork() {}

    public static void sendGlitch(
        ServerWorld world,
        ServerPlayerEntity victim,
        ServerPlayerEntity killer,
        DamageSource damageSource,
        boolean glitchVictim,
        boolean glitchKiller
    ) {
        // Use the same DamageTracker message that vanilla uses for the death line.
        Text deathMessage = victim.getDamageTracker().getDeathMessage();

        String victimName = glitchVictim
            ? victim.getDisplayName().getString()
            : "\u0000never-victim\u0000";

        String killerName = glitchKiller && killer != null
            ? killer.getDisplayName().getString()
            : "\u0000never-killer\u0000";

        Text safeMessage = GlitchTextSanitizer.sanitize(
            deathMessage,
            victimName,
            killerName
        );

        GlitchPayload payload = new GlitchPayload(
            safeMessage,
            GlitchMessageKey.of(deathMessage.getString()),
            victim.getId(),
            glitchVictim,
            glitchKiller
        );

        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, GlitchPayload.ID)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
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
