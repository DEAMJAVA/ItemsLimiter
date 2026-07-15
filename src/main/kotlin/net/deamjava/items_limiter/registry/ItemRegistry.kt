package net.deamjava.items_limiter.registry

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID

object ItemRegistry {

    private val logger = LoggerFactory.getLogger("items-limiter/registry")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val registryFile: File = FabricLoader.getInstance()
        .configDir
        .resolve("items_limiter_registry.json")
        .toFile()

    private val registry: MutableMap<String, MutableList<UUID>> = mutableMapOf()

    fun load() {
        if (!registryFile.exists()) {
            registry.clear()
            return
        }
        try {
            val root = JsonParser.parseReader(registryFile.reader()) as JsonObject
            registry.clear()
            for ((itemId, arr) in root.entrySet()) {
                val uuids = arr.asJsonArray
                    .map { UUID.fromString(it.asString) }
                    .toMutableList()
                registry[itemId] = uuids
            }
            logger.info("Loaded item registry: ${registry.size} tracked items")
        } catch (e: Exception) {
            logger.error("Failed to load item registry", e)
        }
    }

    fun save() {
        try {
            val root = JsonObject()
            for ((itemId, uuids) in registry) {
                val arr = com.google.gson.JsonArray()
                uuids.forEach { arr.add(it.toString()) }
                root.add(itemId, arr)
            }
            registryFile.writeText(gson.toJson(root))
        } catch (e: Exception) {
            logger.error("Failed to save item registry", e)
        }
    }

    fun countFor(itemId: String): Int = registry[itemId]?.size ?: 0


    fun isRegistered(itemId: String, uuid: UUID): Boolean =
        registry[itemId]?.contains(uuid) == true


    fun tryRegister(itemId: String, limit: Int): UUID? {
        val list = registry.getOrPut(itemId) { mutableListOf() }
        if (list.size >= limit) return null
        val uuid = UUID.randomUUID()
        list.add(uuid)
        save()
        return uuid
    }

    fun unregister(itemId: String, uuid: UUID) {
        registry[itemId]?.remove(uuid)
        save()
    }

    fun clearItem(itemId: String) {
        registry.remove(itemId)
        save()
    }
}