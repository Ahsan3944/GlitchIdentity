package in.ultraop.glitchidentity.fabric;

import in.ultraop.glitchidentity.core.GlitchFrameGenerator;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class FabricNetwork {
    public static final Identifier ID = Identifier.of("glitchidentity", "glitch");

    public record GlitchPayload(
        int victimEntityId,
        String victim,
        String killer,
        boolean glitchVictim,
        boolean glitchKiller
    ) implements CustomPayload {
        public static final CustomPayload.Id<GlitchPayload> ID =
            new CustomPayload.Id<>(FabricNetwork.ID);

        public static final PacketCodec<RegistryByteBuf, GlitchPayload> CODEC =
            PacketCodec.tuple(
                PacketCodecs.VAR_INT, GlitchPayload::victimEntityId,
                PacketCodecs.STRING, GlitchPayload::victim,
                PacketCodecs.STRING, GlitchPayload::killer,
                PacketCodecs.BOOLEAN, GlitchPayload::glitchVictim,
                PacketCodecs.BOOLEAN, GlitchPayload::glitchKiller,
                GlitchPayload::new
            );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    static {
        PayloadTypeRegistry.playS2C().register(GlitchPayload.ID, GlitchPayload.CODEC);
    }

    public static void sendGlitch(
        net.minecraft.server.world.ServerWorld world,
        ServerPlayerEntity victim,
        ServerPlayerEntity killer,
        boolean glitchVictim,
        boolean glitchKiller
    ) {
        String safeVictim = glitchVictim ? "" : victim.getName().getString();
        String safeKiller = killer == null || glitchKiller ? "" : killer.getName().getString();

        GlitchPayload payload = new GlitchPayload(
            victim.getId(),
            safeVictim,
            safeKiller,
            glitchVictim,
            glitchKiller
        );

        Text fallback = buildFallbackMessage(victim, killer, glitchVictim, glitchKiller);

        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, GlitchPayload.ID)) {
                ServerPlayNetworking.send(player, payload);
            } else {
                // Vanilla clients do not have the client-side mixin required for animation.
                player.sendMessage(fallback);
            }
        }
    }

    private static Text buildFallbackMessage(
        ServerPlayerEntity victim,
        ServerPlayerEntity killer,
        boolean glitchVictim,
        boolean glitchKiller
    ) {
        GlitchFrameGenerator.Frame victimFrame = glitchVictim
            ? GlitchFrameGenerator.next()
            : null;

        GlitchFrameGenerator.Frame killerFrame = glitchKiller
            ? GlitchFrameGenerator.next()
            : null;

        MutableText message = Text.empty();

        if (glitchVictim) {
            message = message.copy().append(Text.literal(victimFrame.text()));
        } else {
            message = message.copy().append(Text.literal(victim.getName().getString()));
        }

        if (killer != null) {
            message = message.copy().append(Text.literal(" was slain by "));

            if (glitchKiller) {
                message = message.copy().append(Text.literal(killerFrame.text()));
            } else {
                message = message.copy().append(Text.literal(killer.getName().getString()));
            }
        } else {
            message = message.copy().append(Text.literal(" died"));
        }

        return message;
    }
}
