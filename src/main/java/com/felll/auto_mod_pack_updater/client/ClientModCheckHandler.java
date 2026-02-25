package com.felll.auto_mod_pack_updater.client;

import com.felll.auto_mod_pack_updater.AutoModPackUpdater;
import com.felll.auto_mod_pack_updater.network.ModCheckAckPayload;
import com.felll.auto_mod_pack_updater.network.ModCheckPayload;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = AutoModPackUpdater.MODID, value = Dist.CLIENT)
public final class ClientModCheckHandler {

    private ClientModCheckHandler() {
    }

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(AutoModPackUpdater.MODID);
        registrar.configurationToClient(
                ModCheckPayload.TYPE,
                ModCheckPayload.STREAM_CODEC,
                ClientModCheckHandler::handleModCheck);
        registrar.configurationToServer(
                ModCheckAckPayload.TYPE,
                ModCheckAckPayload.STREAM_CODEC,
                (payload, context) -> {});
    }

    private static void handleModCheck(ModCheckPayload payload, IPayloadContext context) {
        context.reply(new ModCheckAckPayload());
    }
}
