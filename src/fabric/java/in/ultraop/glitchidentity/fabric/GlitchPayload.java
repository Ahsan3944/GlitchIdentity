package in.ultraop.glitchidentity.fabric;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;

public record GlitchPayload(
    Text message,
    boolean glitchVictim,
    boolean glitchKiller
) implements CustomPayload {
    public static final Identifier CHANNEL = Identifier.of("glitchidentity", "glitch");

    public static final CustomPayload.Id<GlitchPayload> ID =
        new CustomPayload.Id<>(CHANNEL);

    public static final PacketCodec<RegistryByteBuf, GlitchPayload> CODEC =
        PacketCodec.tuple(
            TextCodecs.REGISTRY_PACKET_CODEC, GlitchPayload::message,
            PacketCodecs.BOOLEAN, GlitchPayload::glitchVictim,
            PacketCodecs.BOOLEAN, GlitchPayload::glitchKiller,
            GlitchPayload::new
        );

    static {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
