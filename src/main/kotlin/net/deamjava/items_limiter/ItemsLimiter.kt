package net.deamjava.items_limiter

import net.deamjava.items_limiter.command.LimiterCommand
import net.deamjava.items_limiter.config.LimiterConfig
import net.deamjava.items_limiter.listener.PlayerJoinListener
import net.deamjava.items_limiter.listener.ServerTickListener
import net.deamjava.items_limiter.registry.ItemRegistry
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object ItemsLimiter : ModInitializer {

	private val logger = LoggerFactory.getLogger("items-limiter")

	override fun onInitialize() {
		logger.info("Initializing Items Limiter")
		ModComponents.init()
		LimiterConfig.load()
		ItemRegistry.load()
		ServerTickListener.register()
		PlayerJoinListener.register()
		LimiterCommand.register()
		logger.info("Items Limiter initialized")
	}
}