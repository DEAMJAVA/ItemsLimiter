package net.deamjava.items_limiter.manager

import net.deamjava.items_limiter.config.LimiterConfig
import net.deamjava.items_limiter.registry.ExclusionRegistry
import net.minecraft.core.Holder
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.ItemEnchantments

object EnchantmentLimitManager {

    fun enchantmentId(holder: Holder<Enchantment>): String =
        holder.unwrapKey().map { it.identifier().toString() }.orElse("unknown")

    fun getConfiguredMax(holder: Holder<Enchantment>): Int? =
        LimiterConfig.getEnchantmentMaxLevel(enchantmentId(holder))

    fun getConfiguredMaxById(id: String): Int? =
        LimiterConfig.getEnchantmentMaxLevel(id)

    fun isBanned(holder: Holder<Enchantment>): Boolean =
        getConfiguredMax(holder) == 0

    fun clampEnchantments(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        if (ExclusionRegistry.isExcluded(stack)) return false
        val enchants = stack.enchantments
        var changed = false

        val mutable = ItemEnchantments.Mutable(enchants)
        for (entry in enchants.entrySet()) {
            val holder = entry.key
            val currentLevel = entry.intValue
            val cap = getConfiguredMax(holder) ?: continue

            if (cap == 0) {
                mutable.set(holder, 0)
                changed = true
            } else if (currentLevel > cap) {
                mutable.set(holder, cap)
                changed = true
            }
        }

        if (changed) {
            EnchantmentHelper.setEnchantments(stack, mutable.toImmutable())
        }
        return changed
    }

    fun scanAndClampPlayer(player: ServerPlayer) {
        val inventory = player.inventory
        for (i in 0 until inventory.containerSize) {
            clampEnchantments(inventory.getItem(i))
        }
        for (slot in EquipmentSlot.entries) {
            clampEnchantments(player.getItemBySlot(slot))
        }
    }
}