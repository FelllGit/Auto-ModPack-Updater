package com.felll.auto_mod_pack_updater.network;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.felll.auto_mod_pack_updater.AutoModPackUpdater;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.common.extensions.IServerConfigurationPacketListenerExtension;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;

public final class ModCheckConfigurationTask implements ICustomConfigurationTask {

    public static final ConfigurationTask.Type TYPE =
            new ConfigurationTask.Type(ResourceLocation.fromNamespaceAndPath(AutoModPackUpdater.MODID, "mod_check"));

    private static final int TIMEOUT_SECONDS = 5;
    private static final ScheduledExecutorService TIMEOUT_EXECUTOR = new ScheduledThreadPoolExecutor(1);
    private static final ConcurrentHashMap<Connection, ModCheckConfigurationTask> PENDING = new ConcurrentHashMap<>();

    private final IServerConfigurationPacketListenerExtension listener;
    private final Connection connection;
    private final AtomicBoolean acknowledged = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> timeoutFuture;

    public ModCheckConfigurationTask(IServerConfigurationPacketListenerExtension listener, Connection connection) {
        this.listener = listener;
        this.connection = connection;
    }

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        PENDING.put(connection, this);
        sender.accept(new ModCheckPayload());
        timeoutFuture = TIMEOUT_EXECUTOR.schedule(() -> {
            if (acknowledged.compareAndSet(false, true)) {
                PENDING.remove(connection);
                connection.disconnect(Component.translatable("automodpackupdater.kick.install_mod"));
            }
        }, TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public static ModCheckConfigurationTask getPending(Connection connection) {
        return PENDING.get(connection);
    }

    @Override
    public ConfigurationTask.Type type() {
        return TYPE;
    }

    public void onAck() {
        if (acknowledged.compareAndSet(false, true)) {
            PENDING.remove(connection);
            ScheduledFuture<?> f = timeoutFuture;
            if (f != null) {
                f.cancel(false);
            }
            listener.finishCurrentTask(TYPE);
        }
    }
}
