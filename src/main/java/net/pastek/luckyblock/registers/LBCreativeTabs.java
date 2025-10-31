package net.pastek.luckyblock.registers;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.pastek.luckyblock.PastekLuckyBlock;

public class LBCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PastekLuckyBlock.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> LUCKY_BLOCK_TAB = CREATIVE_MODE_TAB.register("pastekluckyblock_tab", () -> CreativeModeTab.builder().icon(() -> new ItemStack(LBBlocks.LUCKY_BLOCK.get())).title(Component.translatable("creativetab.pastekluckyblock").withStyle(ChatFormatting.GOLD)).displayItems((itemDisplayParameters, output) -> {

        /** BLOCKS */
        /** Lucky block */
        output.accept(LBBlocks.LUCKY_BLOCK.get());


    }).build());
}