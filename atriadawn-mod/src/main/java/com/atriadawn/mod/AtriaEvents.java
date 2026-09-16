package com.atriadawn.mod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Мост между игровым чатом и нейросетью Atria Dawn:
 * <ul>
 *   <li>сообщения, начинающиеся с префикса из конфига (по умолчанию «@»),
 *       перехватываются и отправляются модели — обычный чат при этом не
 *       рассылается другим игрокам;</li>
 *   <li>команда {@code /atria <сообщение>} — то же самое без префикса;</li>
 *   <li>{@code /atria reset} — забыть контекст диалога;</li>
 *   <li>{@code /atria reload} — перечитать config/atriadawn.json (операторы);</li>
 *   <li>{@code /atria about} — справка.</li>
 * </ul>
 */
public class AtriaEvents {

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        AtriaConfig cfg = AtriaConfig.get();
        String prefix = cfg.chatPrefix == null ? "" : cfg.chatPrefix.strip();
        if (prefix.isEmpty()) {
            return; // триггер префиксом отключён в конфиге — только /atria
        }
        String raw = event.getRawText();
        if (raw == null || !raw.startsWith(prefix)) {
            return;
        }
        event.setCanceled(true); // не рассылаем «@...» как обычный чат

        ServerPlayer player = event.getPlayer();
        String text = raw.substring(prefix.length()).strip();
        if (text.isEmpty()) {
            player.sendSystemMessage(Component.translatable("atria.hint_usage", prefix)
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        AtriaChatManager.ask(player, text);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        AtriaChatManager.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        AtriaCommands.register(event.getDispatcher());
    }
}
