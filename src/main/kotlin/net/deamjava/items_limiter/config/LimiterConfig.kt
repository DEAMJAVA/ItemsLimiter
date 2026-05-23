package net.deamjava.items_limiter.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.io.File

data class ItemLimit(val limit: Int)
data class EnchantmentLimit(val maxLevel: Int)

object LimiterConfig {

    private val logger = LoggerFactory.getLogger("items-limiter/config")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile: File = FabricLoader.getInstance()
        .configDir
        .resolve("items_limiter.json")
        .toFile()

    val itemLimits: MutableMap<String, Int> = mutableMapOf()

    val enchantmentLimits: MutableMap<String, Int> = mutableMapOf()

    fun load() {
        if (!configFile.exists()) {
            writeDefaults()
        }
        try {
            val root = JsonParser.parseReader(configFile.reader()) as JsonObject

            itemLimits.clear()
            root.getAsJsonObject("item_limits")?.entrySet()?.forEach { (key, value) ->
                itemLimits[key] = value.asInt
            }

            enchantmentLimits.clear()
            root.getAsJsonObject("enchantment_limits")?.entrySet()?.forEach { (key, value) ->
                enchantmentLimits[key] = value.asInt
            }

            logger.info("Loaded config: ${itemLimits.size} item limits, ${enchantmentLimits.size} enchantment limits")
        } catch (e: Exception) {
            logger.error("Failed to load config, using defaults", e)
        }
    }

    private fun writeDefaults() {
        val root = JsonObject()

        val items = JsonObject()
        items.addProperty("minecraft:mace", 1)
        root.add("item_limits", items)

        val enchants = JsonObject()
        enchants.addProperty("minecraft:thorns", 0)
        enchants.addProperty("minecraft:protection", 3)
        root.add("enchantment_limits", enchants)

        configFile.writeText(gson.toJson(root))
        logger.info("Wrote default config to ${configFile.path}")
    }

    fun getItemLimit(itemId: String): Int? = itemLimits[itemId]

    fun getEnchantmentMaxLevel(enchantmentId: String): Int? = enchantmentLimits[enchantmentId]
}