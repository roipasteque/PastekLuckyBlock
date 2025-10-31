package net.pastek.luckyblock.registers;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.pastek.luckyblock.PastekLuckyBlock;

public class LBSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, PastekLuckyBlock.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_BADLOOT = registerSoundEvents("badloot");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND_MIDLOOT = registerSoundEvents("midloot");


    private static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvents(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(PastekLuckyBlock.MOD_ID, name)));
    }
}