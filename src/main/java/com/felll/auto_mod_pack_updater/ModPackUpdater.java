package com.felll.auto_mod_pack_updater;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.felll.auto_mod_pack_updater.client.DownloadProgress;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

public final class ModPackUpdater {

    private static final String STATE_FILE = "managed-mods.json";
    private static final Gson GSON = new Gson();

    private ModPackUpdater() {
    }

    public static UpdatePlan computeUpdatePlan(Path gameDirectory, List<String> modFilenames) {
        UpdatePlan plan = new UpdatePlan();
        Path modsDir = gameDirectory.resolve("mods");
        Set<String> manifestFilenames = new HashSet<>(modFilenames);
        for (String filename : modFilenames) {
            Path modPath = modsDir.resolve(filename);
            if (!Files.exists(modPath)) {
                plan.getToAdd().add(filename);
            }
        }
        try {
            try (var stream = Files.list(modsDir)) {
                stream.filter(p -> {
                    if (!Files.isRegularFile(p)) return false;
                    String name = p.getFileName().toString();
                    return name.toLowerCase().endsWith(".jar")
                            && !name.toLowerCase().startsWith("automodpackupdater");
                }).forEach(p -> {
                    String name = p.getFileName().toString();
                    if (!manifestFilenames.contains(name)) {
                        plan.getToRemove().add(name);
                    }
                });
            }
        } catch (IOException e) {
            AutoModPackUpdater.LOGGER.warn("Failed to list mods folder", e);
        }
        return plan;
    }

    public static Set<String> loadManagedMods(Path gameDirectory) {
        Path statePath = ConfigLoader.getConfigDirectory(gameDirectory).resolve(STATE_FILE);
        if (!Files.exists(statePath)) {
            return new HashSet<>();
        }
        try {
            String json = Files.readString(statePath, StandardCharsets.UTF_8);
            JsonArray arr = GSON.fromJson(json, JsonArray.class);
            Set<String> result = new HashSet<>();
            if (arr != null) {
                for (var el : arr) {
                    if (el.isJsonPrimitive()) {
                        result.add(el.getAsString());
                    }
                }
            }
            return result;
        } catch (IOException e) {
            AutoModPackUpdater.LOGGER.warn("Failed to load managed mods state", e);
            return new HashSet<>();
        }
    }

    public static void saveManagedMods(Path gameDirectory, Set<String> managed) {
        try {
            Path configDir = ConfigLoader.getConfigDirectory(gameDirectory);
            Files.createDirectories(configDir);
            JsonArray arr = new JsonArray();
            for (String s : managed) {
                arr.add(s);
            }
            Files.writeString(configDir.resolve(STATE_FILE), GSON.toJson(arr), StandardCharsets.UTF_8);
        } catch (IOException e) {
            AutoModPackUpdater.LOGGER.error("Failed to save managed mods state", e);
        }
    }

    public static List<String> fetchModsFromFolder(String repositoryUrl) throws IOException {
        return ModsFolderFetcher.fetchFromFolder(repositoryUrl);
    }

    public static String getBaseUrlForDownloads(String repositoryUrl) {
        RepoUrlParser.BaseUrlInfo info = RepoUrlParser.parse(repositoryUrl);
        if (info != null) {
            return info.baseUrl() + "mods/";
        }
        String base = repositoryUrl.endsWith("/") ? repositoryUrl : repositoryUrl + "/";
        return base.endsWith("mods/") ? base : base + "mods/";
    }

    public static void downloadMod(String baseUrl, String filename, Path modsDir) throws IOException {
        downloadMod(baseUrl, filename, modsDir, null);
    }

    public static void downloadMod(String baseUrl, String filename, Path modsDir,
            BytesProgressReporter bytesReporter) throws IOException {
        String url = baseUrl.endsWith("/") ? baseUrl + filename : baseUrl + "/" + filename;
        Files.createDirectories(modsDir);
        Path target = modsDir.resolve(filename);
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "AutoModPackUpdater/1.0");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setInstanceFollowRedirects(true);
        long totalBytes = conn.getContentLengthLong();
        if (totalBytes < 0) {
            totalBytes = 0;
        }
        long startTime = System.nanoTime();
        long lastReportTime = startTime;
        try (InputStream in = conn.getInputStream();
                OutputStream out = Files.newOutputStream(target, java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buffer = new byte[8192];
            long bytesDownloaded = 0;
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                bytesDownloaded += n;
                if (bytesReporter != null) {
                    long now = System.nanoTime();
                    if (now - lastReportTime >= 100_000_000) {
                        double elapsedSec = (now - startTime) / 1_000_000_000.0;
                        double bytesPerSecond = elapsedSec > 0 ? bytesDownloaded / elapsedSec : 0;
                        bytesReporter.report(bytesDownloaded, totalBytes, bytesPerSecond);
                        lastReportTime = now;
                    }
                }
            }
        }
    }

    public static void removeMod(Path modsDir, String filename) throws IOException {
        Path target = modsDir.resolve(filename);
        if (Files.exists(target)) {
            Files.delete(target);
        }
    }

    public static void executePlan(Path gameDirectory, String repositoryUrl, List<String> modFilenames, UpdatePlan plan)
            throws IOException {
        executePlan(gameDirectory, repositoryUrl, modFilenames, plan, null);
    }

    public static void executePlan(Path gameDirectory, String repositoryUrl, List<String> modFilenames, UpdatePlan plan,
            Consumer<DownloadProgress> progressCallback) throws IOException {
        Path modsDir = gameDirectory.resolve("mods");
        Set<String> managed = new HashSet<>(loadManagedMods(gameDirectory));
        for (String filename : plan.getToRemove()) {
            removeMod(modsDir, filename);
            managed.remove(filename);
        }
        String baseUrl = getBaseUrlForDownloads(repositoryUrl);
        List<String> toAdd = plan.getToAdd();
        int total = toAdd.size();
        int current = 0;
        for (String filename : toAdd) {
            if (modFilenames.contains(filename)) {
                int cur = ++current;
                BytesProgressReporter bytesReporter = progressCallback != null
                        ? (bytes, totalB, bps) -> progressCallback.accept(new DownloadProgress(cur, total, filename, bytes, totalB, bps))
                        : null;
                if (progressCallback != null) {
                    progressCallback.accept(new DownloadProgress(current, total, filename));
                }
                downloadMod(baseUrl, filename, modsDir, bytesReporter);
                managed.add(filename);
            }
        }
        saveManagedMods(gameDirectory, managed);
    }
}
