package net.pastek.luckyblock.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.pastek.luckyblock.PastekLuckyBlock;
import net.pastek.luckyblock.registers.LBBlocks;

public class LBBlockStateProvider extends BlockStateProvider {

    public LBBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, PastekLuckyBlock.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(LBBlocks.LUCKY_BLOCK);
    }

    private void blockWithItem(DeferredHolder<Block, Block> blockRegistryObject) {
        Block block = blockRegistryObject.get();
        String name = blockRegistryObject.getId().getPath();
        simpleBlock(block, cubeAll(block));
        simpleBlockItem(block, new ModelFile.UncheckedModelFile(modLoc("block/" + name)));
    }

    public ResourceLocation lbTexture(String folder, String texture) {
        return ResourceLocation.fromNamespaceAndPath(PastekLuckyBlock.MOD_ID, "block/" + folder + "/" + texture);
    }

    private void blockItem(DeferredHolder<Block, Block> blockRegistryObject, String loc, String appendix) {
        Block block = blockRegistryObject.get();
        simpleBlockItem(block, new ModelFile.UncheckedModelFile(PastekLuckyBlock.MOD_ID + ":" + loc + blockRegistryObject.getId().getPath() + appendix));
    }
}
