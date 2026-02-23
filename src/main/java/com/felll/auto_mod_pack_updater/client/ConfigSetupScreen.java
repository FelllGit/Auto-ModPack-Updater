package com.felll.auto_mod_pack_updater.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigSetupScreen extends Screen {

    private EditBox repoUrlBox;
    private Button continueButton;
    private String errorMessage;

    public ConfigSetupScreen() {
        super(Component.translatable("screen.automodpackupdater.setup.title"));
    }

    public void setError(String message) {
        this.errorMessage = message;
    }

    @Override
    protected void init() {
        super.init();
        repoUrlBox = new EditBox(font, width / 2 - 150, height / 2 - 30, 300, 20,
                Component.translatable("screen.automodpackupdater.setup.repo_hint"));
        repoUrlBox.setHint(Component.translatable("screen.automodpackupdater.setup.repo_hint"));
        repoUrlBox.setMaxLength(500);
        repoUrlBox.setValue("");
        addRenderableWidget(repoUrlBox);
        setInitialFocus(repoUrlBox);
        continueButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.automodpackupdater.continue"),
                button -> tryContinue())
                .bounds(width / 2 - 100, height / 2 + 20, 200, 20)
                .build());
    }

    private void tryContinue() {
        String url = repoUrlBox.getValue().trim();
        if (url.isEmpty()) {
            setError("screen.automodpackupdater.setup.error_empty");
            return;
        }
        setError(null);
        ModPackUpdateHandler.submitRepoUrl(url);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, height / 2 - 60, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, Component.translatable("screen.automodpackupdater.setup.description"),
                width / 2, height / 2 - 50, 0xAAAAAA);
        if (errorMessage != null) {
            guiGraphics.drawCenteredString(font, Component.translatable(errorMessage), width / 2, height / 2 + 50,
                    0xFF5555);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
