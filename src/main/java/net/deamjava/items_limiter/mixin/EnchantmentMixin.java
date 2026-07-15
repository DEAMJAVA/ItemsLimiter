package net.deamjava.items_limiter.mixin;

import net.deamjava.items_limiter.manager.EnchantmentLimitManager;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
import java.util.stream.Stream;

@Mixin(EnchantmentHelper.class)
public class EnchantmentMixin {

    @Inject(
            method = "getAvailableEnchantmentResults",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void onGetAvailableEnchantmentResults(
            int value,
            ItemStack itemStack,
            Stream<Holder<Enchantment>> source,
            CallbackInfoReturnable<List<EnchantmentInstance>> cir
    ) {
        List<EnchantmentInstance> original = cir.getReturnValue();
        List<EnchantmentInstance> filtered = original.stream()
                .filter(instance -> {
                    Integer cap = EnchantmentLimitManager.INSTANCE.getConfiguredMax(instance.enchantment());
                    return cap == null || cap > 0;
                })
                .map(instance -> {
                    Integer cap = EnchantmentLimitManager.INSTANCE.getConfiguredMax(instance.enchantment());
                    if (cap != null && instance.level() > cap) {
                        return new EnchantmentInstance(instance.enchantment(), cap);
                    }
                    return instance;
                })
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));

        if (filtered.size() != original.size() || !filtered.equals(original)) {
            cir.setReturnValue(filtered);
        }
    }
}