package tr.com.hapicraft.client.ag;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import tr.com.hapicraft.client.HapiClient;

/** hapiclient:ana kanalındaki ham gövde: 1 bayt paket numarası + UTF-8 JSON. */
public record HamPaket(byte[] veri) implements CustomPacketPayload {
    public static final Type<HamPaket> TIP = new Type<>(HapiClient.kimlik("ana"));
    public static final StreamCodec<FriendlyByteBuf, HamPaket> KODEK = StreamCodec.of(
            (buf, paket) -> buf.writeBytes(paket.veri),
            buf -> {
                byte[] veri = new byte[buf.readableBytes()];
                buf.readBytes(veri);
                return new HamPaket(veri);
            });

    @Override
    public Type<HamPaket> type() {
        return TIP;
    }
}
