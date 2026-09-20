package com.atriadawn.mod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
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
        if (cfg.logTriggers) {
            AtriaDawnMod.LOGGER.info("Atria trigger (ServerChatEvent): player='{}' raw='{}' -> queued text='{}'",
                    player.getGameProfile().getName(), raw, text);
        }
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

    private int tickCounter = 0;

    /** Отправляет накопленные вопросы, когда игрок закончил печатать (раз в 0,5 с). */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (++tickCounter < 10) {
            return;
        }
        tickCounter = 0;
        AtriaChatManager.tickFlush();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        AtriaCommands.register(event.getDispatcher());
    }
}
