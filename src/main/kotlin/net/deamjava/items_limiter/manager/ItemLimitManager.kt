package net.deamjava.items_limiter.manager

import net.deamjava.items_limiter.ModComponents
import net.deamjava.items_limiter.config.LimiterConfig
import net.deamjava.items_limiter.registry.ItemRegistry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import org.slf4j.LoggerFactory

object ItemLimitManager {
    private const val DEBUG = false

    private fun debug(msg: String) {
        if (DEBUG) logger.info("[DEBUG] $msg")
    }


    private val worldBounds = AABB(
        -30_000_000.0, -2048.0, -30_000_000.0,
        30_000_000.0, 2048.0, 30_000_000.0
    )

    private val logger = LoggerFactory.getLogger("items-limiter/manager")

    fun itemId(stack: ItemStack): String =
        BuiltInRegistries.ITEM.getKey(stack.item).toString()

    fun isTagged(stack: ItemStack): Boolean =
        ModComponents.hasTag(stack)

    fun tagStack(stack: ItemStack, uuid: java.util.UUID) {
        ModComponents.setTag(stack, uuid)
    }


    fun scanAndCleanPlayer(player: ServerPlayer) {
        debug("scanAndCleanPlayer: starting scan for ${player.name.string}")
        val inventory = player.inventory
        for (i in 0 until inventory.containerSize) {
            val stack = inventory.getItem(i)
            if (stack.isEmpty) continue
            val id = itemId(stack)
            val limit = LimiterConfig.getItemLimit(id) ?: continue

            debug("scanAndCleanPlayer: slot=$i id=$id limit=$limit tagged=${isTagged(stack)}")

            if (limit == 0) {
                debug("scanAndCleanPlayer: slot=$i $id — banned, removing")
                inventory.setItem(i, ItemStack.EMPTY)
                continue
            }

            if (!isTagged(stack)) {
                debug("scanAndCleanPlayer: slot=$i $id — untagged, attempting register (current count=${ItemRegistry.countFor(id)}/$limit)")
                val uuid = ItemRegistry.tryRegister(id, limit)
                if (uuid != null) {
                    tagStack(stack, uuid)
                    debug("scanAndCleanPlayer: slot=$i $id — registered and tagged uuid=$uuid")
                    logger.info("[SCAN] Tagged new $id for ${player.name.string} slot $i | uuid=$uuid")
                } else {
                    debug("scanAndCleanPlayer: slot=$i $id — registry full (count=${ItemRegistry.countFor(id)}/$limit), removing")
                    inventory.setItem(i, ItemStack.EMPTY)
                    logger.info("[SCAN] Removed $id from ${player.name.string} slot $i — limit exhausted")
                }
                continue
            }

            val uuid = ModComponents.getTag(stack)
            debug("scanAndCleanPlayer: slot=$i $id — tagged uuid=$uuid, checking registry")
            if (uuid == null || !ItemRegistry.isRegistered(id, uuid)) {
                debug("scanAndCleanPlayer: slot=$i $id — uuid=$uuid NOT in registry, removing")
                inventory.setItem(i, ItemStack.EMPTY)
                logger.info("[SCAN] Removed $id from ${player.name.string} slot $i — tag not in registry")
            } else {
                debug("scanAndCleanPlayer: slot=$i $id — uuid=$uuid confirmed in registry, KEEP")
            }
        }
        debug("scanAndCleanPlayer: done for ${player.name.string}")
    }

    fun scanAndCleanWorld(server: MinecraftServer) {
        debug("scanAndCleanWorld: starting")
        for (level in server.allLevels) {
            val entities = level.getEntitiesOfClass(ItemEntity::class.java, worldBounds) { true }
            debug("scanAndCleanWorld: level=${level.dimension().identifier()} found ${entities.size} item entities")
            entities.forEach { entity ->
                val stack = entity.item
                if (stack.isEmpty) return@forEach
                val id = itemId(stack)
                val limit = LimiterConfig.getItemLimit(id) ?: return@forEach

                debug("scanAndCleanWorld: $id limit=$limit tagged=${isTagged(stack)}")

                if (limit == 0) {
                    debug("scanAndCleanWorld: $id — banned, discarding")
                    entity.discard()
                    return@forEach
                }

                if (!isTagged(stack)) {
                    debug("scanAndCleanWorld: $id — untagged, attempting register (current count=${ItemRegistry.countFor(id)}/$limit)")
                    val uuid = ItemRegistry.tryRegister(id, limit)
                    if (uuid != null) {
                        tagStack(stack, uuid)
                        debug("scanAndCleanWorld: $id — registered and tagged uuid=$uuid")
                        logger.info("[SCAN] Tagged new $id in world | uuid=$uuid")
                    } else {
                        debug("scanAndCleanWorld: $id — registry full, discarding")
                        entity.discard()
                        logger.info("[SCAN] Discarded $id in world — limit exhausted")
                    }
                    return@forEach
                }

                val uuid = ModComponents.getTag(stack)
                debug("scanAndCleanWorld: $id — tagged uuid=$uuid, checking registry")
                if (uuid == null || !ItemRegistry.isRegistered(id, uuid)) {
                    debug("scanAndCleanWorld: $id — uuid=$uuid NOT in registry, discarding")
                    entity.discard()
                    logger.info("[SCAN] Discarded $id in world — tag not in registry")
                } else {
                    debug("scanAndCleanWorld: $id — uuid=$uuid confirmed in registry, KEEP")
                }
            }
        }
        debug("scanAndCleanWorld: done")
    }

    fun shouldBlockCraft(itemId: String): Boolean {
        val limit = LimiterConfig.getItemLimit(itemId) ?: run {
            debug("shouldBlockCraft: $itemId — not limited, allow")
            return false
        }
        if (limit == 0) {
            debug("shouldBlockCraft: $itemId — banned, block")
            return true
        }
        val count = ItemRegistry.countFor(itemId)
        val block = count >= limit
        debug("shouldBlockCraft: $itemId count=$count limit=$limit → ${if (block) "BLOCK" else "ALLOW"}")
        return block
    }
}