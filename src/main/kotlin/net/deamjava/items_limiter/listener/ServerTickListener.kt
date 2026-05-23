package net.deamjava.items_limiter.listener

import net.deamjava.items_limiter.manager.EnchantmentLimitManager
import net.deamjava.items_limiter.manager.ItemLimitManager
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer

object ServerTickListener {

    private const val SCAN_INTERVAL = 200

    fun register() {
        ServerTickEvents.END_SERVER_TICK.register(::onServerTick)
    }

    private fun onServerTick(server: MinecraftServer) {
        if (server.tickCount % SCAN_INTERVAL != 0) return

        for (player in server.playerList.players) {
            ItemLimitManager.scanAndCleanPlayer(player)
            EnchantmentLimitManager.scanAndClampPlayer(player)
        }

        ItemLimitManager.scanAndCleanWorld(server)
    }
}