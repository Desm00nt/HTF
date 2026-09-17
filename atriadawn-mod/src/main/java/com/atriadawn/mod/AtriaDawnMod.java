package com.atriadawn.mod;

import com.atriadawn.mod.AtriaEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Atria Dawn Chat
 * ---------------
 * Автономный мод для Minecraft (Forge 1.19.2): общение с нейросетевой
 * моделью <b>Atria Dawn</b> прямо в игровом чате, на русском языке.
 *
 * <p>Никак не зависит от других модов. Возможности:</p>
 * <ul>
 *   <li>сообщение в чате, начинающееся с «@» (настраивается), уходит
 *       нейросети — обычный чат не рассылается;</li>
 *   <li>команда {@code /atria <сообщение>} — то же самое без префикса;</li>
 *   <li>{@code /atria key <ключ>} — вписать API-ключ прямо в игре;</li>
 *   <li>{@code /atria reset} — забыть контекст, {@code /atria reload} —
 *       перечитать конфиг, {@code /atria about} — справка.</li>
 * </ul>
 *
 * <p>По умолчанию используется официальный OpenAI-совместимый API
 * {@code https://api.atria-asi.ai/v1/chat/completions} и модель
 * {@code Atria-Dawn-Preview}; подходит любой совместимый эндпоинт.</p>
 */
@Mod(AtriaDawnMod.MOD_ID)
public class AtriaDawnMod {

    public static final String MOD_ID = "atriadawn";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public AtriaDawnMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(new AtriaEvents());

        LOGGER.info("Atria Dawn Chat loaded - write \"@<question>\" in chat or use /atria");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Регистрация сети не требуется: мод общается с внешним HTTP API
        // напрямую и не имеет собственных пакетов клиент<->сервер.
    }
}
