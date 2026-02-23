package com.felll.auto_mod_pack_updater;

import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public record ModManifest(List<ModEntry> mods) {

    private static final Gson GSON = new Gson();

    public ModManifest {
        mods = mods != null ? List.copyOf(mods) : Collections.emptyList();
    }

    public static ModManifest parse(String json) throws JsonParseException {
        if (json == null || json.isBlank()) {
            throw new JsonParseException("Empty manifest response");
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("<") || trimmed.startsWith("<!")) {
            throw new JsonParseException("Received HTML instead of JSON (check URL, file may not exist)");
        }
        JsonElement element = JsonParser.parseString(json);
        if (!element.isJsonObject()) {
            throw new JsonParseException("Manifest must be a JSON object, got: " + element.getClass().getSimpleName());
        }
        JsonObject root = element.getAsJsonObject();
        JsonArray modsArray = root.getAsJsonArray("mods");
        if (modsArray == null) {
            return new ModManifest(Collections.emptyList());
        }
        List<ModEntry> entries = new java.util.ArrayList<>();
        for (JsonElement modEl : modsArray) {
            if (modEl.isJsonObject()) {
                JsonObject obj = modEl.getAsJsonObject();
                String filename = getString(obj, "filename");
                String url = getString(obj, "url");
                String hash = getString(obj, "hash");
                if (filename != null) {
                    entries.add(new ModEntry(filename, url, hash));
                }
            }
        }
        return new ModManifest(entries);
    }

    private static String getString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : null;
    }

    public record ModEntry(String filename, String url, String hash) {
    }
}
