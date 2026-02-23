package com.felll.auto_mod_pack_updater.client;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.felll.auto_mod_pack_updater.AutoModPackUpdater;
import com.felll.auto_mod_pack_updater.ConfigLoader;
import com.felll.auto_mod_pack_updater.RepoUrlParser;
import com.felll.auto_mod_pack_updater.ModPackUpdater;
import com.felll.auto_mod_pack_updater.UpdatePlan;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = AutoModPackUpdater.MODID, value = Dist.CLIENT)
public final class ModPackUpdateHandler {

    private static volatile boolean allowTitleScreen;
    private static volatile boolean hasRunInitialCheck;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AutoModPackUpdater");
        t.setDaemon(true);
        return t;
    });

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getScreen() instanceof TitleScreen)) {
            return;
        }
        if (allowTitleScreen) {
            allowTitleScreen = false;
            return;
        }
        if (hasRunInitialCheck) {
            return;
        }
        hasRunInitialCheck = true;
        Minecraft mc = Minecraft.getInstance();
        Path gameDir = mc.gameDirectory.toPath();
        Optional<String> repoUrlOpt = ConfigLoader.loadRepositoryUrl(gameDir);
        if (repoUrlOpt.isEmpty()) {
            event.setCanceled(true);
            mc.setScreen(new ConfigSetupScreen());
            return;
        }
        String repoUrl = repoUrlOpt.get();
        event.setCanceled(true);
        AtomicReference<String> statusRef = new AtomicReference<>("screen.automodpackupdater.status.fetching");
        mc.setScreen(new ModUpdaterScreen(List.of(), List.of(), false, null, null, repoUrl, statusRef));
        EXECUTOR.submit(() -> runUpdateFlow(mc, gameDir, repoUrl, statusRef));
    }

    public static void submitRepoUrl(String url) {
        Minecraft mc = Minecraft.getInstance();
        Path gameDir = mc.gameDirectory.toPath();
        RepoUrlParser.BaseUrlInfo info = RepoUrlParser.parse(url);
        if (info == null) {
            ConfigSetupScreen screen = new ConfigSetupScreen();
            screen.setError("screen.automodpackupdater.setup.error_invalid");
            mc.setScreen(screen);
            return;
        }
        AtomicReference<String> statusRef = new AtomicReference<>("screen.automodpackupdater.status.saving");
        mc.setScreen(new ModUpdaterScreen(List.of(), List.of(), false, null, null, url, statusRef));
        EXECUTOR.submit(() -> {
            try {
                ConfigLoader.saveRepositoryUrl(gameDir, url);
                statusRef.set("screen.automodpackupdater.status.fetching");
                runUpdateFlow(mc, gameDir, url, statusRef);
            } catch (Exception e) {
                AutoModPackUpdater.LOGGER.error("Failed to save config", e);
                mc.execute(() -> {
                    ConfigSetupScreen screen = new ConfigSetupScreen();
                    screen.setError("screen.automodpackupdater.setup.error_save");
                    mc.setScreen(screen);
                });
            }
        });
    }

    private static void runUpdateFlow(Minecraft mc, Path gameDir, String repoUrl, AtomicReference<String> statusRef) {
        try {
            if (statusRef != null) {
                statusRef.set("screen.automodpackupdater.status.fetching");
            }
            var mods = ModPackUpdater.fetchModsFromFolder(repoUrl);
            if (statusRef != null) {
                statusRef.set("screen.automodpackupdater.status.computing");
            }
            UpdatePlan plan = ModPackUpdater.computeUpdatePlan(gameDir, mods);
            if (!plan.hasChanges()) {
                allowTitleScreen = true;
                mc.execute(() -> mc.setScreen(new TitleScreen(false)));
                return;
            }
            List<String> added = new ArrayList<>(plan.getToAdd());
            AtomicReference<DownloadProgress> progressRef = new AtomicReference<>();
            mc.execute(() -> mc.setScreen(new ModUpdaterScreen(added, plan.getToRemove(), false, null, progressRef)));
            ModPackUpdater.executePlan(gameDir, repoUrl, mods, plan, progressRef::set);
            mc.execute(() -> mc.setScreen(new ModUpdaterScreen(added, plan.getToRemove(), true, null)));
        } catch (Exception e) {
            AutoModPackUpdater.LOGGER.error("Mod pack update failed", e);
            String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            mc.execute(() -> mc.setScreen(new ModUpdaterScreen(List.of(), List.of(), true, errMsg)));
        }
    }
}
