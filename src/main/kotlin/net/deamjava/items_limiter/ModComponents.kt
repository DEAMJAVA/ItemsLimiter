package net.deamjava.items_limiter

import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.resources.Identifier
import java.util.UUID
import com.mojang.serialization.Codec

object ModComponents {

    val LIMITER_TAG: DataComponentType<UUID> = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath("items-limiter", "limiter_tag"),
        DataComponentType.builder<UUID>()
            .persistent(Codec.STRING.xmap(UUID::fromString, UUID::toString))
            .networkSynchronized(ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString))
            .build()
    )

    fun init() {

    }
}