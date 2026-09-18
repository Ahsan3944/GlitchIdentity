package in.ultraop.glitchidentity.fabric.mixin;

import in.ultraop.glitchidentity.fabric.GlitchIdentityFabricClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(
        method = "onDeathMessage(Lnet/minecraft/network/packet/s2c/play/DeathMessageS2CPacket;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void glitchIdentity$replaceDeathMessage(
        DeathMessageS2CPacket packet,
        CallbackInfo ci
    ) {
        if (GlitchIdentityFabricClient.shouldReplaceDeathMessage(packet.playerId())) {
            ci.cancel();
        }
    }
}
