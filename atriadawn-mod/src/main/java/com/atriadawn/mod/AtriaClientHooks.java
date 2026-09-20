package com.atriadawn.mod;

import net.minecraft.client.multiplayer.chat.ChatPreviewStatus;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Клиентские хуки мода (загружаются только на клиенте).
 *
 * <p>Главный хук: принудительно выключаем ванильный «Предпросмотр чата»
 * (Chat Preview). С включённым предпросмотром клиент шлёт серверу запрос
 * декорирования на <b>каждое нажатие клавиши</b> в чате; на некоторых
 * серверах/гибридах эти заготовки доходят до обработчиков чата, из-за чего
 * бот «отвечает на отдельные буквы» и заспамливает запросы к API. Отключение
 * предпросмотра отсекает проблему в корне. Опция выключается при каждом
 * входе в мир и сохраняется в options.txt.</p>
 */
@Mod.EventBusSubscriber(modid = AtriaDawnMod.MOD_ID, value = Dist.CLIENT)
public class AtriaClientHooks {

    @SubscribeEvent
    public static void onLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null && mc.options.chatPreview().get() != ChatPreviewStatus.OFF) {
            mc.options.chatPreview().set(ChatPreviewStatus.OFF);
            mc.options.save();
            AtriaDawnMod.LOGGER.info("Atria Dawn: ванильный «Предпросмотр чата» выключен (источник ложных запросов бота)");
        }
    }
}
