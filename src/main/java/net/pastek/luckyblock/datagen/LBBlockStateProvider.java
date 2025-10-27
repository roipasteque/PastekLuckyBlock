package net.pastek.luckyblock.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import net.pastek.luckyblock.PastekLuckyBlock;
import net.pastek.luckyblock.registers.LBBlocks;


public class LBBlockStateProvider extends BlockStateProvider {

    public LBBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, PastekLuckyBlock.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockItem(LBBlocks.LUCKY_BLOCK);
    }


    private void blockWithItem(RegistryObject<Block> blockRegistryObject) {
        String path = blockRegistryObject.getId().getPath();
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));
    }
    public ResourceLocation LBTexture(String folder, String texture) {
        return ResourceLocation.fromNamespaceAndPath(PastekLuckyBlock.MOD_ID, "block/" + folder + "/" + texture);}
    private void blockItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlockItem(blockRegistryObject.get(), new ModelFile.UncheckedModelFile("pastekluckyblock:block/" + blockRegistryObject.getId().getPath()));}
    private void blockItem(RegistryObject<Block> blockRegistryObject, String loc, String appendix) {
        simpleBlockItem(blockRegistryObject.get(), new ModelFile.UncheckedModelFile("pastekluckyblock:" + loc + blockRegistryObject.getId().getPath() + appendix));}
}