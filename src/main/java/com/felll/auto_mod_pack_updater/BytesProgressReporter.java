package com.felll.auto_mod_pack_updater;

@FunctionalInterface
public interface BytesProgressReporter {
    void report(long bytesDownloaded, long totalBytes, double bytesPerSecond);
}
