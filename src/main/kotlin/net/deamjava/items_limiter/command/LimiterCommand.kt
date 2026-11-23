package net.deamjava.items_limiter.command

import com.mojang.brigadier.arguments.IntegerArgumentType
import net.deamjava.items_limiter.ModComponents
import net.deamjava.items_limiter.config.LimiterConfig
import net.deamjava.items_limiter.manager.EnchantmentLimitManager
import net.deamjava.items_limiter.manager.ItemLimitManager
import net.deamjava.items_limiter.registry.ExclusionRegistry
import net.deamjava.items_limiter.registry.ItemRegistry
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.ResourceArgument
import net.minecraft.commands.synchronization.SuggestionProviders
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import java.util.UUID

object LimiterCommand {

    private val isAdmin = { source: net.minecraft.commands.CommandSourceStack ->
        Commands.LEVEL_ADMINS.check(source.permissions())
    }

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, _ ->
            dispatcher.register(
                Commands.literal("itemslimiter")
                    .requires(isAdmin)
                    .then(
                        Commands.literal("item")
                            .then(
                                Commands.literal("set")
                                    .then(
                                        Commands.argument("item", ResourceArgument.resource(registryAccess, Registries.ITEM))
                                            .then(
                                                Commands.argument("limit", IntegerArgumentType.integer(0))
                                                    .executes { ctx ->
                                                        val itemHolder = ResourceArgument.getResource(ctx, "item", Registries.ITEM)
                                                        val itemId = itemHolder.key().identifier().toString()
                                                        val limit = IntegerArgumentType.getInteger(ctx, "limit")
                                                        LimiterConfig.itemLimits[itemId] = limit
                                                        LimiterConfig.save()
                                                        ctx.source.sendSuccess(
                                                            { Component.literal("Set item limit for $itemId to $limit") },
                                                            true
                                                        )
                                                        1
                                                    }
                                            )
                                    )
                            )
                            .then(
                                Commands.literal("remove")
                                    .then(
                                        Commands.argument("item", ResourceArgument.resource(registryAccess, Registries.ITEM))
                                            .executes { ctx ->
                                                val itemHolder = ResourceArgument.getResource(ctx, "item", Registries.ITEM)
                                                val itemId = itemHolder.key().identifier().toString()
                                                LimiterConfig.itemLimits.remove(itemId)
                                                LimiterConfig.save()
                                                ctx.source.sendSuccess(
                                                    { Component.literal("Removed item limit for $itemId") },
                                                    true
                                                )
                                                1
                                            }
                                    )
                            )
                            .then(
                                Commands.literal("reset")
                                    .then(
                                        Commands.argument("item", ResourceArgument.resource(registryAccess, Registries.ITEM))
                                            .executes { ctx ->
                                                val itemHolder = ResourceArgument.getResource(ctx, "item", Registries.ITEM)
                                                val itemId = itemHolder.key().identifier().toString()
                                                ItemRegistry.clearItem(itemId)
                                                ctx.source.sendSuccess(
                                                    { Component.literal("Reset registry for $itemId — existing tagged copies cleared, new ones can now be registered.") },
                                                    true
                                                )
                                                1
                                            }
                                    )
                            )
                            .then(
                                Commands.literal("list")
                                    .executes { ctx ->
                                        if (LimiterConfig.itemLimits.isEmpty()) {
                                            ctx.source.sendSuccess(
                                                { Component.literal("No item limits configured.") },
                                                false
                                            )
                                        } else {
                                            val lines = LimiterConfig.itemLimits.entries
                                                .joinToString("\n") { (id, limit) ->
                                                    val display = if (limit == 0) "BANNED" else "max $limit (${ItemRegistry.countFor(id)} registered)"
                                                    "  $id -> $display"
                                                }
                                            ctx.source.sendSuccess(
                                                { Component.literal("Item limits:\n$lines") },
                                                false
                                            )
                                        }
                                        1
                                    }
                            )
                    )
                    .then(
                        Commands.literal("enchantment")
                            .then(
                                Commands.literal("set")
                                    .then(
                                        Commands.argument("enchantment", ResourceArgument.resource(registryAccess, Registries.ENCHANTMENT))
                                            .then(
                                                Commands.argument("max_level", IntegerArgumentType.integer(0))
                                                    .executes { ctx ->
                                                        val enchHolder = ResourceArgument.getResource(ctx, "enchantment", Registries.ENCHANTMENT)
                                                        val enchId = enchHolder.key().identifier().toString()
                                                        val maxLevel = IntegerArgumentType.getInteger(ctx, "max_level")
                                                        LimiterConfig.enchantmentLimits[enchId] = maxLevel
                                                        LimiterConfig.save()
                                                        ctx.source.sendSuccess(
                                                            { Component.literal("Set enchantment max level for $enchId to $maxLevel (0 = banned)") },
                                                            true
                                                        )
                                                        1
                                                    }
                                            )
                                    )
                            )
                            .then(
                                Commands.literal("remove")
                                    .then(
                                        Commands.argument("enchantment", ResourceArgument.resource(registryAccess, Registries.ENCHANTMENT))
                                            .executes { ctx ->
                                                val enchHolder = ResourceArgument.getResource(ctx, "enchantment", Registries.ENCHANTMENT)
                                                val enchId = enchHolder.key().identifier().toString()
                                                LimiterConfig.enchantmentLimits.remove(enchId)
                                                LimiterConfig.save()
                                                ctx.source.sendSuccess(
                                                    { Component.literal("Removed enchantment limit for $enchId") },
                                                    true
                                                )
                                                1
                                            }
                                    )
                            )
                            .then(
                                Commands.literal("list")
                                    .executes { ctx ->
                                        if (LimiterConfig.enchantmentLimits.isEmpty()) {
                                            ctx.source.sendSuccess(
                                                { Component.literal("No enchantment limits configured.") },
                                                false
                                            )
                                        } else {
                                            val lines = LimiterConfig.enchantmentLimits.entries
                                                .joinToString("\n") { (id, level) ->
                                                    val display = if (level == 0) "BANNED" else "max $level"
                                                    "  $id -> $display"
                                                }
                                            ctx.source.sendSuccess(
                                                { Component.literal("Enchantment limits:\n$lines") },
                                                false
                                            )
                                        }
                                        1
                                    }
                            )
                    )
                    .then(
                        Commands.literal("reload")
                            .executes { ctx ->
                                LimiterConfig.load()
                                ctx.source.sendSuccess(
                                    { Component.literal("Items Limiter config reloaded from disk.") },
                                    true
                                )
                                1
                            }
                    )
                    .then(
                        Commands.literal("scan")
                            .executes { ctx ->
                                val server = ctx.source.server
                                for (player in server.playerList.players) {
                                    ItemLimitManager.scanAndCleanPlayer(player)
                                    EnchantmentLimitManager.scanAndClampPlayer(player)
                                }
                                ItemLimitManager.scanAndCleanWorld(server)
                                ctx.source.sendSuccess(
                                    { Component.literal("Scan complete.") },
                                    true
                                )
                                1
                            }
                    )
                    .then(
                        Commands.literal("exclude")
                            // /itemslimiter exclude  -> excludes the item in your main hand
                            .executes { ctx ->
                                val player = ctx.source.playerOrException
                                val stack = player.mainHandItem
                                if (stack.isEmpty) {
                                    ctx.source.sendFailure(Component.literal("You must hold an item."))
                                    return@executes 0
                                }

                                val itemId = ItemLimitManager.itemId(stack)
                                var uuid = ModComponents.getTag(stack)

                                if (uuid == null) {
                                    val limit = LimiterConfig.getItemLimit(itemId)
                                    uuid = (if (limit != null && limit > 0) ItemRegistry.tryRegister(itemId, limit) else null)
                                        ?: UUID.randomUUID()
                                    ModComponents.setTag(stack, uuid)
                                }

                                if (ExclusionRegistry.add(uuid)) {
                                    ctx.source.sendSuccess(
                                        { Component.literal("Excluded held $itemId (id $uuid). It will be ignored by all limits.") },
                                        true
                                    )
                                } else {
                                    ctx.source.sendSuccess(
                                        { Component.literal("This item is already excluded.") },
                                        false
                                    )
                                }
                                1
                            }
                            .then(
                                Commands.literal("remove")
                                    .executes { ctx ->
                                        val player = ctx.source.playerOrException
                                        val uuid = ModComponents.getTag(player.mainHandItem)
                                        if (uuid != null && ExclusionRegistry.remove(uuid)) {
                                            ctx.source.sendSuccess(
                                                { Component.literal("Held item is no longer excluded; limits apply again.") },
                                                true
                                            )
                                            1
                                        } else {
                                            ctx.source.sendFailure(Component.literal("Held item is not excluded."))
                                            0
                                        }
                                    }
                            )
                            .then(
                                Commands.literal("list")
                                    .executes { ctx ->
                                        ctx.source.sendSuccess(
                                            { Component.literal("${ExclusionRegistry.count()} excluded item(s).") },
                                            false
                                        )
                                        1
                                    }
                            )
                    )
            )
        }
    }
}