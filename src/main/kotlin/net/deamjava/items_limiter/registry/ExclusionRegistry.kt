package net.deamjava.items_limiter.registry

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.deamjava.items_limiter.ModComponents
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID

object ExclusionRegistry {

    private val logger = LoggerFactory.getLogger("items-limiter/exclusions")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file: File = FabricLoader.getInstance()
        .configDir
        .resolve("items_limiter_exclusions.json")
        .toFile()

    private val excluded: MutableSet<UUID> = mutableSetOf()

    fun load() {
        excluded.clear()
        if (!file.exists()) return
        try {
            val root = JsonParser.parseReader(file.reader()) as JsonObject
            root.getAsJsonArray("excluded")?.forEach { excluded.add(UUID.fromString(it.asString)) }
            logger.info("Loaded ${excluded.size} excluded items")
        } catch (e: Exception) {
            logger.error("Failed to load exclusions", e)
        }
    }

    fun save() {
        try {
            val root = JsonObject()
            val arr = JsonArray()
            excluded.forEach { arr.add(it.toString()) }
            root.add("excluded", arr)
            file.writeText(gson.toJson(root))
        } catch (e: Exception) {
            logger.error("Failed to save exclusions", e)
        }
    }

    fun add(uuid: UUID): Boolean = excluded.add(uuid).also { if (it) save() }

    fun remove(uuid: UUID): Boolean = excluded.remove(uuid).also { if (it) save() }

    fun count(): Int = excluded.size

    fun isExcluded(uuid: UUID): Boolean = uuid in excluded

    fun isExcluded(stack: net.minecraft.world.item.ItemStack): Boolean {
        if (stack.isEmpty) return false
        val uuid = ModComponents.getTag(stack) ?: return false
        return uuid in excluded
    }
}