package com.felll.auto_mod_pack_updater;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class ModsFolderFetcher {

    private static final String MODS_FOLDER = "mods";

    private ModsFolderFetcher() {
    }

    public static List<ModManifest.ModEntry> fetchFromFolder(String repositoryUrl) throws IOException {
        RepoUrlParser.RepoInfo info = RepoUrlParser.parseForApi(repositoryUrl);
        if (info == null) {
            throw new IOException("Unsupported repository URL. Use GitHub, GitLab, Gitea, or Codeberg.");
        }
        return switch (info.provider()) {
            case "github" -> fetchFromGitHub(info);
            case "gitlab" -> fetchFromGitLab(info);
            case "codeberg", "gitea" -> fetchFromGitea(info);
            default -> throw new IOException("Unsupported provider: " + info.provider());
        };
    }

    private static List<ModManifest.ModEntry> fetchFromGitHub(RepoUrlParser.RepoInfo info) throws IOException {
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents/%s?ref=%s",
                info.owner(), info.repo(), MODS_FOLDER, info.branch());
        String json = fetchUrl(apiUrl, "application/vnd.github.v3+json");
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonArray()) {
            JsonObject obj = root.getAsJsonObject();
            if (obj.has("message")) {
                throw new IOException("mods/ folder not found: " + obj.get("message").getAsString());
            }
            return List.of();
        }
        return parseGitHubResponse(root.getAsJsonArray());
    }

    private static List<ModManifest.ModEntry> parseGitHubResponse(JsonArray items) {
        List<ModManifest.ModEntry> mods = new ArrayList<>();
        for (JsonElement el : items) {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                String type = getStr(obj, "type");
                String name = getStr(obj, "name");
                if ("file".equals(type) && name != null && name.toLowerCase().endsWith(".jar")) {
                    String url = getStr(obj, "download_url");
                    mods.add(new ModManifest.ModEntry(name, url, null));
                }
            }
        }
        return mods;
    }

    private static List<ModManifest.ModEntry> fetchFromGitLab(RepoUrlParser.RepoInfo info) throws IOException {
        String projectPath = info.owner() + "%2F" + info.repo();
        String apiUrl = String.format("https://gitlab.com/api/v4/projects/%s/repository/tree?path=%s&ref=%s&per_page=100",
                projectPath, MODS_FOLDER, info.branch());
        String json = fetchUrl(apiUrl, "application/json");
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonArray()) {
            JsonObject obj = root.getAsJsonObject();
            if (obj.has("message")) {
                throw new IOException("mods/ folder not found: " + obj.get("message").getAsString());
            }
            return List.of();
        }
        String rawBase = String.format("https://gitlab.com/%s/%s/-/raw/%s/%s/",
                info.owner(), info.repo(), info.branch(), MODS_FOLDER);
        return parseGitLabResponse(root.getAsJsonArray(), rawBase);
    }

    private static List<ModManifest.ModEntry> parseGitLabResponse(JsonArray items, String rawBase) {
        List<ModManifest.ModEntry> mods = new ArrayList<>();
        for (JsonElement el : items) {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                String type = getStr(obj, "type");
                String name = getStr(obj, "name");
                if ("blob".equals(type) && name != null && name.toLowerCase().endsWith(".jar")) {
                    String url = rawBase + URLEncoder.encode(name, StandardCharsets.UTF_8);
                    mods.add(new ModManifest.ModEntry(name, url, null));
                }
            }
        }
        return mods;
    }

    private static List<ModManifest.ModEntry> fetchFromGitea(RepoUrlParser.RepoInfo info) throws IOException {
        String baseUrl = "https://" + info.host();
        String apiUrl = String.format("%s/api/v1/repos/%s/%s/contents/%s?ref=%s",
                baseUrl, info.owner(), info.repo(), MODS_FOLDER, info.branch());
        String json = fetchUrl(apiUrl, "application/json");
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonArray()) {
            JsonObject obj = root.getAsJsonObject();
            if (obj.has("message")) {
                throw new IOException("mods/ folder not found: " + obj.get("message").getAsString());
            }
            return List.of();
        }
        String rawBase = String.format("%s/%s/%s/raw/branch/%s/%s/",
                baseUrl, info.owner(), info.repo(), info.branch(), MODS_FOLDER);
        return parseGiteaResponse(root.getAsJsonArray(), rawBase);
    }

    private static List<ModManifest.ModEntry> parseGiteaResponse(JsonArray items, String rawBase) {
        List<ModManifest.ModEntry> mods = new ArrayList<>();
        for (JsonElement el : items) {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                String type = getStr(obj, "type");
                String name = getStr(obj, "name");
                if ("file".equals(type) && name != null && name.toLowerCase().endsWith(".jar")) {
                    String url = rawBase + URLEncoder.encode(name, StandardCharsets.UTF_8);
                    mods.add(new ModManifest.ModEntry(name, url, null));
                }
            }
        }
        return mods;
    }

    private static String getStr(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : null;
    }

    private static String fetchUrl(String urlString, String accept) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(urlString).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", accept);
        conn.setRequestProperty("User-Agent", "AutoModPackUpdater/1.0");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setInstanceFollowRedirects(true);
        int code = conn.getResponseCode();
        if (code != 200) {
            try (InputStream err = conn.getErrorStream()) {
                String body = err != null ? new String(err.readAllBytes(), StandardCharsets.UTF_8) : "";
                throw new IOException("HTTP " + code + ": " + body);
            }
        }
        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
