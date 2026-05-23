package net.deamjava.items_limiter.listener

import net.deamjava.items_limiter.manager.EnchantmentLimitManager
import net.deamjava.items_limiter.manager.ItemLimitManager
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents

object PlayerJoinListener {

    fun register() {
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val player = handler.player
            player.level().server.execute {
                ItemLimitManager.scanAndCleanPlayer(player)
                EnchantmentLimitManager.scanAndClampPlayer(player)
            }
        }
    }
}