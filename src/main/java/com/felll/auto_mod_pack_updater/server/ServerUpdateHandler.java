package com.felll.auto_mod_pack_updater.server;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.felll.auto_mod_pack_updater.client.DownloadProgress;

import com.felll.auto_mod_pack_updater.AutoModPackUpdater;
import com.felll.auto_mod_pack_updater.ConfigLoader;
import com.felll.auto_mod_pack_updater.ModPackUpdater;
import com.felll.auto_mod_pack_updater.UpdatePlan;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = AutoModPackUpdater.MODID, value = Dist.DEDICATED_SERVER)
public final class ServerUpdateHandler {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AutoModPackUpdater-Server");
        t.setDaemon(true);
        return t;
    });

    private ServerUpdateHandler() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        Path gameDir = server.getServerDirectory();
        Optional<String> repoUrlOpt = ConfigLoader.loadRepositoryUrl(gameDir);
        if (repoUrlOpt.isEmpty()) {
            AutoModPackUpdater.LOGGER.info("Skipping server mod update: no repo URL. Create {}/config.txt with your repository URL.",
                    ConfigLoader.getConfigDirectory(gameDir));
            return;
        }
        String repoUrl = repoUrlOpt.get();
        EXECUTOR.submit(() -> runServerUpdate(server, gameDir, repoUrl));
    }

    private static void runServerUpdate(MinecraftServer server, Path gameDir, String repoUrl) {
        try {
            var mods = ModPackUpdater.fetchModsFromServerFolder(repoUrl);
            UpdatePlan plan = ModPackUpdater.computeUpdatePlan(gameDir, mods);
            if (!plan.hasChanges()) {
                AutoModPackUpdater.LOGGER.info("Server mods are up to date (server/ folder has {} mods).", mods.size());
                return;
            }
            int toAdd = plan.getToAdd().size();
            int toRemove = plan.getToRemove().size();
            AutoModPackUpdater.LOGGER.info("Server mod update: adding {} mods, removing {} mods.", toAdd, toRemove);
            ModPackUpdater.executeServerPlan(gameDir, repoUrl, mods, plan, p ->
                    AutoModPackUpdater.LOGGER.info("Downloading server mod {}/{}: {}", p.current(), p.total(), p.filename()));
            AutoModPackUpdater.LOGGER.info("Server mods updated from server/ folder. Restarting server.");
            server.execute(() -> {
                server.getPlayerList().broadcastSystemMessage(
                        Component.translatable("automodpackupdater.server.restart.line1"), false);
                server.getPlayerList().broadcastSystemMessage(
                        Component.translatable("automodpackupdater.server.restart.line2"), false);
                server.getPlayerList().broadcastSystemMessage(
                        Component.translatable("automodpackupdater.server.restart.line3"), false);
                server.halt(false);
            });
        } catch (Exception e) {
            AutoModPackUpdater.LOGGER.error("Server mod pack update failed", e);
        }
    }
}
