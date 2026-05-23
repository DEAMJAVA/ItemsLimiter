package net.deamjava.items_limiter.manager

import net.deamjava.items_limiter.ModComponents
import net.deamjava.items_limiter.config.LimiterConfig
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import java.util.UUID

object ItemLimitManager {

    fun itemId(stack: ItemStack): String =
        BuiltInRegistries.ITEM.getKey(stack.item).toString()

    fun isTagged(stack: ItemStack): Boolean =
        stack.has(ModComponents.LIMITER_TAG)

    fun tagStack(stack: ItemStack) {
        stack.set(ModComponents.LIMITER_TAG, UUID.randomUUID())
    }

    fun countTagged(server: MinecraftServer, itemId: String): Int {
        var count = 0

        for (player in server.playerList.players) {
            for (i in 0 until player.inventory.containerSize) {
                val stack = player.inventory.getItem(i)
                if (!stack.isEmpty && itemId(stack) == itemId && isTagged(stack)) {
                    count++
                }
            }
        }

        for (level in server.allLevels) {
            level.getEntitiesOfClass(ItemEntity::class.java,
                net.minecraft.world.phys.AABB(
                    -30_000_000.0, -2048.0, -30_000_000.0,
                    30_000_000.0, 2048.0, 30_000_000.0
                )
            ) { true }.forEach { entity ->
                val stack = entity.item
                if (itemId(stack) == itemId && isTagged(stack)) {
                    count++
                }
            }
        }

        return count
    }

    fun scanAndCleanPlayer(player: net.minecraft.server.level.ServerPlayer) {
        val inventory = player.inventory
        for (i in 0 until inventory.containerSize) {
            val stack = inventory.getItem(i)
            if (stack.isEmpty) continue

            val id = itemId(stack)
            val limit = LimiterConfig.getItemLimit(id) ?: continue

            if (limit == 0) {
                // fully banned — remove regardless of tag
                inventory.setItem(i, ItemStack.EMPTY)
            } else if (!isTagged(stack)) {
                // untagged instance of a limited item — remove
                inventory.setItem(i, ItemStack.EMPTY)
            }
        }
    }

    fun scanAndCleanWorld(server: MinecraftServer) {
        for (level in server.allLevels) {
            val toRemove = mutableListOf<ItemEntity>()
            for (entity in level.getEntitiesOfClass(ItemEntity::class.java,
                level.levelData.run {
                    // use a large enough AABB to cover loaded chunks
                    val r = (level.maxY + 64).toDouble()
                    net.minecraft.world.phys.AABB(-30_000_000.0, -r, -30_000_000.0, 30_000_000.0, r, 30_000_000.0)
                }
            ) { true }) {
                val stack = entity.item
                val id = itemId(stack)
                val limit = LimiterConfig.getItemLimit(id) ?: continue

                if (limit == 0 || !isTagged(stack)) {
                    toRemove.add(entity)
                }
            }
            toRemove.forEach { it.discard() }
        }
    }

    fun shouldBlockCraft(server: MinecraftServer, result: ItemStack): Boolean {
        val id = itemId(result)
        val limit = LimiterConfig.getItemLimit(id) ?: return false
        if (limit == 0) return true
        return countTagged(server, id) >= limit
    }
}