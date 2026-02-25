package com.felll.auto_mod_pack_updater.network;

import com.felll.auto_mod_pack_updater.AutoModPackUpdater;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ModCheckAckPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ModCheckAckPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AutoModPackUpdater.MODID, "mod_check_ack"));

    public static final StreamCodec<ByteBuf, ModCheckAckPayload> STREAM_CODEC =
            StreamCodec.unit(new ModCheckAckPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
