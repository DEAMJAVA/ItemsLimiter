package net.deamjava.items_limiter.mixin;

import net.deamjava.items_limiter.manager.EnchantmentLimitManager;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {

    @Inject(method = "createResult", at = @At("TAIL"))
    private void onCreateResult(CallbackInfo ci) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        net.minecraft.world.item.ItemStack result = self.getSlot(2).getItem();
        if (result.isEmpty()) return;

        EnchantmentLimitManager.INSTANCE.clampEnchantments(result);
    }
}