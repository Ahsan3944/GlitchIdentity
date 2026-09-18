package in.ultraop.glitchidentity.fabric;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class FabricNetwork {
    public static final Identifier ID = Identifier.of("glitchidentity", "glitch");

    public record GlitchPayload(
        String victim,
        String killer,
        boolean glitchVictim,
        boolean glitchKiller
    ) implements CustomPayload {
        public static final CustomPayload.Id<GlitchPayload> ID =
            new CustomPayload.Id<>(FabricNetwork.ID);

        public static final PacketCodec<RegistryByteBuf, GlitchPayload> CODEC =
            PacketCodec.tuple(
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
            safeVictim,
            safeKiller,
            glitchVictim,
            glitchKiller
        );

        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, GlitchPayload.ID)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
}
