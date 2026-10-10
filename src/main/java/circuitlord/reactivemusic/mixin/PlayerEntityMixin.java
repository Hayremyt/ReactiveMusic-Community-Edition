package circuitlord.reactivemusic.mixin;

import circuitlord.reactivemusic.SongPicker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void reactivemusic$onAttack(Entity target, CallbackInfo ci) {
        // item frames, boats, passive mobs and the like don't count, only real combatants
        if ((Object) this instanceof ClientPlayerEntity && isCombatant(target)) {
            SongPicker.registerCombatHit();
        }
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void reactivemusic$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ClientPlayerEntity)) return;
        if (source.getAttacker() == null || source.getAttacker() == (Object) this) return;
        if (!isCombatant(source.getAttacker())) return;

        SongPicker.registerCombatHit();
        SongPicker.registerDamageTaken(amount);
    }

    private static boolean isCombatant(Entity entity) {
        return entity instanceof HostileEntity || entity instanceof PlayerEntity;
    }

}
