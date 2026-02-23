package com.felll.auto_mod_pack_updater;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(AutoModPackUpdater.MODID)
public class AutoModPackUpdater {

    public static final String MODID = "automodpackupdater";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AutoModPackUpdater(IEventBus modEventBus, ModContainer modContainer) {
    }
}
