package net.pastek.luckyblock.datagen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.pastek.luckyblock.PastekLuckyBlock;

public class LBItemModelProvider extends ItemModelProvider {
    public LBItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, PastekLuckyBlock.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {

    }
}