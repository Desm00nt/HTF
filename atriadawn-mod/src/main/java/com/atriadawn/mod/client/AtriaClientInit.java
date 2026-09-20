package com.atriadawn.mod.client;

import com.atriadawn.mod.AtriaDawnMod;
import com.atriadawn.mod.client.renderer.AtriaCompanionRenderer;
import com.atriadawn.mod.registry.AtriaRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Клиентская регистрация: рендерер компаньона. */
@Mod.EventBusSubscriber(modid = AtriaDawnMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AtriaClientInit {

    private AtriaClientInit() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AtriaRegistry.ATRIA_COMPANION.get(), AtriaCompanionRenderer::new);
    }
}
