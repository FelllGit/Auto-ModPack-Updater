package com.felll.auto_mod_pack_updater.network;

import com.felll.auto_mod_pack_updater.AutoModPackUpdater;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ModCheckPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ModCheckPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AutoModPackUpdater.MODID, "mod_check"));

    public static final StreamCodec<ByteBuf, ModCheckPayload> STREAM_CODEC =
            StreamCodec.unit(new ModCheckPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
