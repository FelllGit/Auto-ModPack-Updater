package com.felll.auto_mod_pack_updater.server;

import com.felll.auto_mod_pack_updater.AutoModPackUpdater;
import com.felll.auto_mod_pack_updater.network.ModCheckAckPayload;
import com.felll.auto_mod_pack_updater.network.ModCheckConfigurationTask;
import com.felll.auto_mod_pack_updater.network.ModCheckPayload;

import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = AutoModPackUpdater.MODID, value = Dist.DEDICATED_SERVER)
public final class ServerModCheckHandler {

    private ServerModCheckHandler() {
    }

    @SubscribeEvent
    public static void onRegisterConfigurationTasks(RegisterConfigurationTasksEvent event) {
        var listener = event.getListener();
        Connection connection = null;
        if (listener instanceof ServerCommonPacketListenerImpl common) {
            connection = common.getConnection();
        }
        if (connection != null && listener instanceof net.neoforged.neoforge.common.extensions.IServerConfigurationPacketListenerExtension ext) {
            event.register(new ModCheckConfigurationTask(ext, connection));
        }
    }

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(AutoModPackUpdater.MODID);
        registrar.configurationToClient(
                ModCheckPayload.TYPE,
                ModCheckPayload.STREAM_CODEC,
                (payload, context) -> {});
        registrar.configurationToServer(
                ModCheckAckPayload.TYPE,
                ModCheckAckPayload.STREAM_CODEC,
                ServerModCheckHandler::handleModCheckAck);
    }

    private static void handleModCheckAck(ModCheckAckPayload payload, IPayloadContext context) {
        Connection connection = context.connection();
        var task = ModCheckConfigurationTask.getPending(connection);
        if (task != null) {
            task.onAck();
        }
    }
}
