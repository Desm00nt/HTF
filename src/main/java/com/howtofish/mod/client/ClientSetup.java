package com.howtofish.mod.client;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.client.model.FishModel;
import com.howtofish.mod.client.model.OldManModel;
import com.howtofish.mod.client.renderer.BossFishRenderer;
import com.howtofish.mod.client.renderer.CustomFishRenderer;
import com.howtofish.mod.client.renderer.OldManRenderer;
import com.howtofish.mod.registry.ModEntities;
import com.howtofish.mod.registry.ModMenuTypes;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterMenuScreensEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod.EventBusSubscriber(modid = HowToFishMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Reserved for future client-only init (item properties, key bindings, etc.)
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CUSTOM_FISH.get(), CustomFishRenderer::new);
        event.registerEntityRenderer(ModEntities.BOSS_FISH.get(), BossFishRenderer::new);
        event.registerEntityRenderer(ModEntities.OLD_MAN.get(), OldManRenderer::new);
        event.registerBlockEntityRenderer(com.howtofish.mod.registry.ModBlockEntities.LIGHTHOUSE_LAMP.get(),
                com.howtofish.mod.client.renderer.LighthouseLampRenderer::new);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(OldManModel.LAYER_LOCATION, OldManModel::createBodyLayer);
        event.registerLayerDefinition(FishModel.LAYER_LOCATION, FishModel::createBodyLayer);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.OLD_MAN_SHOP.get(), OldManShopScreen::new);
    }
}
