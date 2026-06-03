package net.deamjava.items_limiter

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import org.slf4j.LoggerFactory
import java.util.UUID

object ModComponents {

    private const val TAG_KEY = "items_limiter_tag"

    private const val DEBUG = false

    private fun debug(msg: String) {
        if (DEBUG) logger.info("[DEBUG] $msg")
    }

    private val logger = LoggerFactory.getLogger("items-limiter/mod-components")

    fun getTag(stack: ItemStack): UUID? {
        val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return null
        val tag = customData.copyTag()
        if (!tag.contains(TAG_KEY)) return null
        return try {
            val raw = (tag.get(TAG_KEY) as? net.minecraft.nbt.StringTag)?.value ?: return null
            UUID.fromString(raw)
        } catch (e: IllegalArgumentException) {
            logger.warn("getTag: failed to parse UUID from '${ tag.getString(TAG_KEY) }'")
            null
        }
    }

    fun setTag(stack: ItemStack, uuid: UUID) {
        val existing = stack.get(DataComponents.CUSTOM_DATA)?.copyTag() ?: CompoundTag()
        existing.putString(TAG_KEY, uuid.toString())
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(existing))
    }

    fun hasTag(stack: ItemStack): Boolean = getTag(stack) != null

    fun init() {
    }
}