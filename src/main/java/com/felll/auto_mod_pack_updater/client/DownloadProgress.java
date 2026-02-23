package com.felll.auto_mod_pack_updater.client;

public record DownloadProgress(int current, int total, String filename, long bytesDownloaded, long totalBytes, double bytesPerSecond) {
    public DownloadProgress(int current, int total, String filename) {
        this(current, total, filename, 0, 0, 0);
    }
}
