package in.ultraop.glitchidentity.fabric.mixin;

import in.ultraop.glitchidentity.fabric.GlitchIdentityFabric;
import in.ultraop.glitchidentity.fabric.GlitchPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void glitchidentity$prepareDeath(
        DamageSource damageSource,
        CallbackInfo ci
    ) {
        GlitchIdentityFabric.prepareDeath(
            (ServerPlayerEntity) (Object) this,
            damageSource
        );
    }

    @Redirect(
        method = "onDeath",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"
        )
    )
    private void glitchidentity$broadcastSanitized(
        PlayerManager playerManager,
        Text message,
        boolean overlay
    ) {
        ServerPlayerEntity victim = (ServerPlayerEntity) (Object) this;
        Text safeMessage = GlitchIdentityFabric.consumeDeathReplacement(
            victim.getUuid()
        );

        if (safeMessage == null) {
            playerManager.broadcast(message, overlay);
            return;
        }

        playerManager.broadcast(
            message,
            recipient -> ServerPlayNetworking.canSend(recipient, GlitchPayload.ID)
                ? safeMessage
                : message,
            overlay
        );
    }
}
