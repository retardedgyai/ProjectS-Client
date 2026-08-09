package io.github.gyai.projects.client;

import io.github.gyai.projects.client.vfx.AbilityVfx;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Fail-closed Fabric boundary for the framed ability VFX protocol. */
public record AbilityVfxPayload(AbilityVfx.Decoded decoded) implements CustomPacketPayload {
    public static final Type<AbilityVfxPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("projects","ability_vfx_v1"));
    public static final StreamCodec<FriendlyByteBuf,AbilityVfxPayload> CODEC=CustomPacketPayload.codec(AbilityVfxPayload::write,AbilityVfxPayload::read);
    private static AbilityVfxPayload read(FriendlyByteBuf b){try{int n=b.readableBytes();if(n>AbilityVfx.MAX_PACKET){b.skipBytes(n);return new AbilityVfxPayload(AbilityVfx.Decoded.invalid());}byte[] bytes=new byte[n];b.readBytes(bytes);return new AbilityVfxPayload(AbilityVfx.decode(bytes));}catch(RuntimeException e){b.skipBytes(b.readableBytes());return new AbilityVfxPayload(AbilityVfx.Decoded.invalid());}}
    private void write(FriendlyByteBuf b){throw new UnsupportedOperationException("Ability VFX is clientbound only");}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
