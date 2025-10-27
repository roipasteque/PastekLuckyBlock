package net.pastek.luckyblock.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.pastek.luckyblock.PastekLuckyBlock;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class LBDatapackProvider extends DatapackBuiltinEntriesProvider {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder();

    public LBDatapackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(PastekLuckyBlock.MOD_ID));
    }
}