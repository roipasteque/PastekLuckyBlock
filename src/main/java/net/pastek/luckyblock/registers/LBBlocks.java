package net.pastek.luckyblock.registers;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.pastek.luckyblock.PastekLuckyBlock;
import net.pastek.luckyblock.common.block.luckyblock.LuckyBlock;

import java.util.function.Supplier;

public class LBBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.createBlocks(PastekLuckyBlock.MOD_ID);

    public static final DeferredBlock<Block> LUCKY_BLOCK = registerBlock("lucky_block",
            () -> new LuckyBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(2f).noOcclusion().noLootTable()));


    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = (DeferredBlock<T>) BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
       LBItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
