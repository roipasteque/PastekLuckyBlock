package net.pastek.luckyblock.registers;


import net.neoforged.bus.api.IEventBus;

public class UnifiedLuckyBlockRegister {

    public static void register(IEventBus eventBus) {
        LBBlocks.BLOCKS.register(eventBus);
        LBItems.ITEMS.register(eventBus);
        LBSounds.SOUNDS.register(eventBus);
        LBLootModifiers.LOOT_MODIFIER_SERIALIZERS.register(eventBus);
        LBCreativeTabs.CREATIVE_MODE_TAB.register(eventBus);
    }
}
