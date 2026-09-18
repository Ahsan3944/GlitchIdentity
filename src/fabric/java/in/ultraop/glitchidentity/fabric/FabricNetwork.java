package in.ultraop.glitchidentity.fabric;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;

public final class FabricNetwork {
    public static final Identifier ID = Identifier.of("glitchidentity", "glitch");
    public static final CustomPayload.Type<GlitchPayload> GLITCH = new CustomPayload.Type<>(ID);

    public record GlitchPayload(String victim, String killer) implements CustomPayload {
        public static final com.mojang.serialization.Codec<GlitchPayload> CODEC = null;
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryByteBuf, GlitchPayload> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.composite(PacketCodecs.STRING, GlitchPayload::victim, PacketCodecs.STRING, GlitchPayload::killer, GlitchPayload::new);
        @Override public CustomPayload.Type<? extends CustomPayload> getType() { return GLITCH; }
    }

    static {
        PayloadTypeRegistry.playS2C().register(GLITCH, GlitchPayload.STREAM_CODEC);
    }

    public static void sendGlitch(net.minecraft.server.world.ServerWorld world, ServerPlayerEntity victim, String killer) {
        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(p, GLITCH)) ServerPlayNetworking.send(p, new GlitchPayload(victim.getName().getString(), killer));
        }
    }
}
