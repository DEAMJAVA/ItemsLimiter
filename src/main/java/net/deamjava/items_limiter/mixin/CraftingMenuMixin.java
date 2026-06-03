package net.deamjava.items_limiter.mixin;

import net.deamjava.items_limiter.manager.ItemLimitManager;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public class CraftingMenuMixin {

    @Inject(
            method = "slotChangedCraftingGrid",
            at = @At("TAIL")
    )
    private static void onSlotChangedCraftingGrid(
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer container,
            ResultContainer resultSlots,
            @Nullable RecipeHolder<CraftingRecipe> recipeHint,
            CallbackInfo ci
    ) {
        ItemStack result = resultSlots.getItem(0);
        if (result.isEmpty()) return;

        String id = ItemLimitManager.INSTANCE.itemId(result);
        boolean blocked = ItemLimitManager.INSTANCE.shouldBlockCraft(id);

        if (blocked) {
            resultSlots.setItem(0, ItemStack.EMPTY);
            menu.setRemoteSlot(0, ItemStack.EMPTY);
            if (player instanceof ServerPlayer sp) {
                sp.connection.send(new ClientboundContainerSetSlotPacket(
                        menu.containerId, menu.incrementStateId(), 0, ItemStack.EMPTY
                ));
            }
        }
    }
}