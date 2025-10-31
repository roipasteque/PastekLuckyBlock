package net.pastek.luckyblock.registers;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.pastek.luckyblock.PastekLuckyBlock;

public class LBItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, PastekLuckyBlock.MOD_ID);
}
