package com.felll.auto_mod_pack_updater.client;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModUpdaterScreen extends Screen {

    private static final int LINE_HEIGHT = 12;
    private static final int LIST_TOP = 100;
    private static final int LIST_PADDING = 20;

    private final List<String> added;
    private final List<String> removed;
    private final boolean completed;
    private final String errorMessage;
    private final AtomicReference<DownloadProgress> progressRef;
    private final String repoUrl;
    private final AtomicReference<String> statusRef;
    private double scrollOffset;
    private int listHeight;

    public ModUpdaterScreen(List<String> added, List<String> removed, boolean completed, String errorMessage) {
        this(added, removed, completed, errorMessage, null, null, null);
    }

    public ModUpdaterScreen(List<String> added, List<String> removed, boolean completed, String errorMessage,
            AtomicReference<DownloadProgress> progressRef) {
        this(added, removed, completed, errorMessage, progressRef, null, null);
    }

    public ModUpdaterScreen(List<String> added, List<String> removed, boolean completed, String errorMessage,
            AtomicReference<DownloadProgress> progressRef, String repoUrl, AtomicReference<String> statusRef) {
        super(Component.translatable("screen.automodpackupdater.title"));
        this.added = added != null ? List.copyOf(added) : List.of();
        this.removed = removed != null ? List.copyOf(removed) : List.of();
        this.completed = completed;
        this.errorMessage = errorMessage;
        this.progressRef = progressRef;
        this.repoUrl = repoUrl;
        this.statusRef = statusRef;
    }

    @Override
    protected void init() {
        super.init();
        if (completed || errorMessage != null) {
            addRenderableWidget(Button.builder(
                    Component.translatable("gui.automodpackupdater.restart"),
                    button -> Minecraft.getInstance().stop())
                    .bounds(width / 2 - 100, height - 32, 200, 20)
                    .build());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseY >= LIST_TOP && mouseY <= height - 40) {
            double maxScroll = Math.max(0, listHeight - (height - LIST_TOP - 40));
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - scrollY * 20));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int y = 20;
        if (errorMessage != null) {
            guiGraphics.drawCenteredString(font, Component.literal(errorMessage), width / 2, y, 0xFF5555);
        } else if (completed) {
            guiGraphics.drawCenteredString(font, Component.translatable("screen.automodpackupdater.complete"),
                    width / 2, y, 0x55FF55);
        } else if (added.isEmpty() && removed.isEmpty()) {
            String status = statusRef != null ? statusRef.get() : null;
            if (status != null) {
                guiGraphics.drawCenteredString(font, Component.translatable(status), width / 2, y, 0xFFFFFF);
            } else {
                guiGraphics.drawCenteredString(font, Component.translatable("screen.automodpackupdater.checking"),
                        width / 2, y, 0xFFFFFF);
            }
            y += 14;
            if (repoUrl != null && !repoUrl.isEmpty()) {
                String displayUrl = repoUrl.length() > 60 ? repoUrl.substring(0, 57) + "..." : repoUrl;
                guiGraphics.drawCenteredString(font, displayUrl, width / 2, y, 0xAAAAAA);
            }
        } else {
            guiGraphics.drawCenteredString(font, Component.translatable("screen.automodpackupdater.updating"),
                    width / 2, y, 0xFFFFFF);
            y += 20;
            DownloadProgress progress = progressRef != null ? progressRef.get() : null;
            if (progress != null && progress.total() > 0) {
                String progressText = Component.translatable("screen.automodpackupdater.downloading",
                        progress.current(), progress.total(), progress.filename()).getString();
                guiGraphics.drawCenteredString(font, progressText, width / 2, y, 0x55FF55);
                y += 14;
                int barWidth = width - 80;
                int filled = progress.total() > 0 ? (barWidth * progress.current()) / progress.total() : 0;
                guiGraphics.fill(width / 2 - barWidth / 2, y, width / 2 + barWidth / 2, y + 8, 0xFF333333);
                guiGraphics.fill(width / 2 - barWidth / 2, y, width / 2 - barWidth / 2 + filled, y + 8, 0xFF55FF55);
                y += 16;
                if (progress.bytesPerSecond() > 0) {
                    double mbitPerSec = (progress.bytesPerSecond() * 8) / 1_000_000;
                    guiGraphics.drawCenteredString(font,
                            Component.translatable("screen.automodpackupdater.speed",
                                    String.format("%.2f", mbitPerSec)),
                            width / 2, y, 0x55AAFF);
                }
            }
        }
        if (!added.isEmpty() || !removed.isEmpty()) {
            int listTop = LIST_TOP;
            int listBottom = height - 40;
            listHeight = 0;
            if (!added.isEmpty()) {
                listHeight += 4 + 14 + added.size() * LINE_HEIGHT + 8;
            }
            if (!removed.isEmpty()) {
                listHeight += 14 + removed.size() * LINE_HEIGHT;
            }
            guiGraphics.enableScissor(LIST_PADDING, listTop, width - LIST_PADDING, listBottom);
            int contentY = listTop - (int) scrollOffset;
            boolean showRemovedFirst = !removed.isEmpty();
            if (showRemovedFirst) {
                contentY += 4;
                guiGraphics.drawString(font, Component.translatable("screen.automodpackupdater.removing"), LIST_PADDING,
                        contentY, 0xFF5555);
                contentY += 14;
                for (String mod : removed) {
                    guiGraphics.drawString(font, "- " + mod, LIST_PADDING + 10, contentY, 0xFF5555);
                    contentY += LINE_HEIGHT;
                }
                contentY += 8;
            }
            if (!added.isEmpty()) {
                contentY += showRemovedFirst ? 0 : 4;
                guiGraphics.drawString(font, Component.translatable("screen.automodpackupdater.adding"), LIST_PADDING,
                        contentY, 0x55FF55);
                contentY += 14;
                int currentMod = progressRef != null && progressRef.get() != null ? progressRef.get().current() : 0;
                for (int i = 0; i < added.size(); i++) {
                    String mod = added.get(i);
                    int modIndex = i + 1;
                    int color;
                    String prefix;
                    if (completed || modIndex < currentMod) {
                        color = 0x55FF55;
                        prefix = "[+] ";
                    } else if (modIndex == currentMod) {
                        color = 0xFFFF55;
                        prefix = ">>> ";
                    } else {
                        color = 0x888888;
                        prefix = "[ ] ";
                    }
                    guiGraphics.drawString(font, prefix + mod, LIST_PADDING + 10, contentY, color);
                    contentY += LINE_HEIGHT;
                }
                contentY += 8;
            }
            if (!removed.isEmpty() && !showRemovedFirst) {
                guiGraphics.drawString(font, Component.translatable("screen.automodpackupdater.removing"), LIST_PADDING,
                        contentY, 0xFF5555);
                contentY += 14;
                int removedColor = completed ? 0xFF5555 : 0xAAAAAA;
                for (String mod : removed) {
                    guiGraphics.drawString(font, "- " + mod, LIST_PADDING + 10, contentY, removedColor);
                    contentY += LINE_HEIGHT;
                }
            }
            guiGraphics.disableScissor();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
