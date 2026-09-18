package in.ultraop.glitchidentity.fabric;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class FabricNetwork {
    public static final Identifier ID = Identifier.of("glitchidentity", "glitch");
    public static final CustomPayload.Type<GlitchPayload> GLITCH = new CustomPayload.Type<>(ID);

    public record GlitchPayload(
        String victim,
        String killer,
        boolean glitchVictim,
        boolean glitchKiller
    ) implements CustomPayload {
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryByteBuf, GlitchPayload> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.composite(
                PacketCodecs.STRING, GlitchPayload::victim,
                PacketCodecs.STRING, GlitchPayload::killer,
                PacketCodecs.BOOL, GlitchPayload::glitchVictim,
                PacketCodecs.BOOL, GlitchPayload::glitchKiller,
                GlitchPayload::new
            );

        @Override
        public CustomPayload.Type<? extends CustomPayload> getType() {
            return GLITCH;
        }
    }

    static {
        PayloadTypeRegistry.playS2C().register(GLITCH, GlitchPayload.STREAM_CODEC);
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
            safeVictim,
            safeKiller,
            glitchVictim,
            glitchKiller
        );

        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, GLITCH)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
}
