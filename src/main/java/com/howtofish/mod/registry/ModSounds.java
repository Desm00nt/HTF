package com.howtofish.mod.registry;

import com.howtofish.mod.HowToFishMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, HowToFishMod.MOD_ID);

    public static final RegistryObject<SoundEvent> OLD_MAN_EAT = registerSound("old_man_eat");
    public static final RegistryObject<SoundEvent> OLD_MAN_TALK = registerSound("old_man_talk");
    public static final RegistryObject<SoundEvent> COIN = registerSound("coin");
    public static final RegistryObject<SoundEvent> FISH_FLOP = registerSound("fish_flop");
    public static final RegistryObject<SoundEvent> BOSS_ROAR = registerSound("boss_roar");

    private static RegistryObject<SoundEvent> registerSound(String name) {
        ResourceLocation id = new ResourceLocation(HowToFishMod.MOD_ID, name);
        return SOUND_EVENTS.register(name, () -> new SoundEvent(id));
    }
}
