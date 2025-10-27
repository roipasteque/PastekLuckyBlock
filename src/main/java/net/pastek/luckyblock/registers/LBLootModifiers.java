package net.pastek.luckyblock.registers;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.bus.api.IEventBus;
import net.pastek.luckyblock.PastekLuckyBlock;
import net.pastek.luckyblock.common.loot.LootTableModifier;

import java.util.function.Supplier;

public class LBLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, PastekLuckyBlock.MOD_ID);

    public static final Supplier<MapCodec<? extends IGlobalLootModifier>> LOOT_TABLE =
            LOOT_MODIFIER_SERIALIZERS.register("loot_table", () -> LootTableModifier.CODEC);

    public static void register(IEventBus eventBus) {
        LOOT_MODIFIER_SERIALIZERS.register(eventBus);
    }
}
