package com.felll.auto_mod_pack_updater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class ConfigLoader {

    private static final String CONFIG_DIR = "mods/auto-mod-pack-updater";
    private static final String CONFIG_FILE = "config.txt";

    private ConfigLoader() {
    }

    public static Path getConfigDirectory(Path gameDirectory) {
        return gameDirectory.resolve(CONFIG_DIR);
    }

    public static Path getConfigPath(Path gameDirectory) {
        return getConfigDirectory(gameDirectory).resolve(CONFIG_FILE);
    }

    public static Optional<String> loadRepositoryUrl(Path gameDirectory) {
        Path configPath = getConfigPath(gameDirectory);
        if (!Files.exists(configPath)) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(configPath);
            String url = content.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .findFirst()
                    .orElse(null);
            return Optional.ofNullable(url);
        } catch (IOException e) {
            AutoModPackUpdater.LOGGER.error("Failed to read config from {}", configPath, e);
            return Optional.empty();
        }
    }

    public static void saveRepositoryUrl(Path gameDirectory, String url) throws IOException {
        Path configDir = getConfigDirectory(gameDirectory);
        Files.createDirectories(configDir);
        Files.writeString(getConfigPath(gameDirectory), url.trim() + "\n", java.nio.charset.StandardCharsets.UTF_8);
    }
}
