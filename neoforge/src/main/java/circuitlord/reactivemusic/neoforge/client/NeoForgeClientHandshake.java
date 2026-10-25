package circuitlord.reactivemusic.neoforge.client;

import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.client.handshake.ClientHandshakeManager;
import circuitlord.reactivemusic.client.handshake.HandshakeConstants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.UUID;

/**
 * Клиентская сетевая интеграция с серверным модом для NeoForge (payload-API).
 * Идентификаторы payload'ов и кодеки совпадают с серверным модом.
 */
@EventBusSubscriber(modid = "reactivemusic", value = Dist.CLIENT)
public class NeoForgeClientHandshake {

    /** Регистрация payload'ов (вызывается из mod event bus). */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();

        registrar.playToServer(HandshakeRequestPayload.TYPE, HandshakeRequestPayload.CODEC,
                NeoForgeClientHandshake::handleNoop);
        registrar.playToClient(HandshakeResponsePayload.TYPE, HandshakeResponsePayload.CODEC,
                NeoForgeClientHandshake::handleResponse);
        registrar.playToClient(MobTargetPayload.TYPE, MobTargetPayload.CODEC,
                NeoForgeClientHandshake::handleMobTarget);
        registrar.playToClient(StructurePayload.TYPE, StructurePayload.CODEC,
                NeoForgeClientHandshake::handleStructure);

        ClientHandshakeManager manager = SongPicker.clientHandshakeManager;
        manager.setRequestSender(version ->
                PacketDistributor.sendToServer(new HandshakeRequestPayload(version)));
    }

    private static void handleNoop(CustomPayload payload, IPayloadContext ctx) {
    }

    private static void handleResponse(HandshakeResponsePayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> SongPicker.clientHandshakeManager.onServerResponse(msg.serverVersion()));
    }

    private static void handleMobTarget(MobTargetPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> SongPicker.onMobTargetPacket(msg.mobUuid(), msg.targetUuid()));
    }

    private static void handleStructure(StructurePayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> SongPicker.onStructurePacket(msg.structures()));
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        SongPicker.clientHandshakeManager.onPlayerJoin("1.3.5");
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SongPicker.clientHandshakeManager.onPlayerLeave();
        SongPicker.clientHandshakeManager.clear();
    }

    // ---------------- payload'ы (идентичны серверным) ----------------

    public record HandshakeRequestPayload(String clientVersion) implements CustomPayload {
        public static final CustomPayload.Id<HandshakeRequestPayload> TYPE =
                new CustomPayload.Id<>(Identifier.tryParse(HandshakeConstants.CHANNEL_HANDSHAKE_REQUEST));
        public static final PacketCodec<RegistryByteBuf, HandshakeRequestPayload> CODEC = PacketCodec.ofStatic(
                (RegistryByteBuf buf, HandshakeRequestPayload v) -> buf.writeString(v.clientVersion()),
                buf -> new HandshakeRequestPayload(buf.readString()));
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return TYPE; }
    }

    public record HandshakeResponsePayload(String serverVersion) implements CustomPayload {
        public static final CustomPayload.Id<HandshakeResponsePayload> TYPE =
                new CustomPayload.Id<>(Identifier.tryParse(HandshakeConstants.CHANNEL_HANDSHAKE_RESPONSE));
        public static final PacketCodec<RegistryByteBuf, HandshakeResponsePayload> CODEC = PacketCodec.ofStatic(
                (RegistryByteBuf buf, HandshakeResponsePayload v) -> buf.writeString(v.serverVersion()),
                buf -> new HandshakeResponsePayload(buf.readString()));
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return TYPE; }
    }

    public record MobTargetPayload(UUID mobUuid, UUID targetUuid) implements CustomPayload {
        public static final CustomPayload.Id<MobTargetPayload> TYPE =
                new CustomPayload.Id<>(Identifier.tryParse(HandshakeConstants.CHANNEL_TARGETS));
        public static final PacketCodec<RegistryByteBuf, MobTargetPayload> CODEC = PacketCodec.ofStatic(
                (RegistryByteBuf buf, MobTargetPayload v) -> {
                    buf.writeUuid(v.mobUuid());
                    if (v.targetUuid() == null) buf.writeBoolean(false);
                    else { buf.writeBoolean(true); buf.writeUuid(v.targetUuid()); }
                },
                buf -> {
                    UUID mob = buf.readUuid();
                    boolean has = buf.readBoolean();
                    UUID target = has ? buf.readUuid() : null;
                    return new MobTargetPayload(mob, target);
                });
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return TYPE; }
    }

    public record StructurePayload(String[] structures) implements CustomPayload {
        public static final CustomPayload.Id<StructurePayload> TYPE =
                new CustomPayload.Id<>(Identifier.tryParse(HandshakeConstants.CHANNEL_STRUCTURES));
        public static final PacketCodec<RegistryByteBuf, StructurePayload> CODEC = PacketCodec.ofStatic(
                (RegistryByteBuf buf, StructurePayload v) -> {
                    buf.writeInt(v.structures().length);
                    for (String s : v.structures()) buf.writeString(s);
                },
                buf -> {
                    int len = buf.readInt();
                    String[] arr = new String[len];
                    for (int i = 0; i < len; i++) arr[i] = buf.readString();
                    return new StructurePayload(arr);
                });
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return TYPE; }
    }
}
