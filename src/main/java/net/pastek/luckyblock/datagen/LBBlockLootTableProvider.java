package net.pastek.luckyblock.datagen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.pastek.luckyblock.registers.LBBlocks;

import java.util.Set;

public class LBBlockLootTableProvider extends BlockLootSubProvider {

    public LBBlockLootTableProvider(HolderLookup.Provider registryProvider) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registryProvider);
    }

    @Override
    protected void generate() {

    }

    protected LootTable.Builder createMultipleOreDrops(Block block, Item item) {
        Holder<Enchantment> fortune = this.registries.lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.FORTUNE);
        return createSilkTouchDispatchTable(block,
                this.applyExplosionDecay(block,
                        LootItem.lootTableItem(item)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))
                                .apply(ApplyBonusCount.addOreBonusCount(fortune))
                )
        );
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return LBBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
