package circuitlord.reactivemusic.fabric.client;

import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.client.handshake.ClientHandshakeManager;
import circuitlord.reactivemusic.client.handshake.HandshakeConstants;
import circuitlord.reactivemusic.client.network.MobTargetStatus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;

//? if >=1.20.5 {
/*import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;
*///?} else {
import circuitlord.reactivemusic.client.network.HandshakePackets;
import circuitlord.reactivemusic.client.network.ServerDataPackets;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
//?}

public class FabricClientHandshake implements ClientModInitializer {

    //? if >=1.20.5 {
    /*private static final Identifier ID_HANDSHAKE_REQUEST = Identifier.tryParse(HandshakeConstants.CHANNEL_HANDSHAKE_REQUEST);
    private static final Identifier ID_HANDSHAKE_RESPONSE = Identifier.tryParse(HandshakeConstants.CHANNEL_HANDSHAKE_RESPONSE);
    private static final Identifier ID_TARGETS = Identifier.tryParse(HandshakeConstants.CHANNEL_TARGETS);
    private static final Identifier ID_STRUCTURES = Identifier.tryParse(HandshakeConstants.CHANNEL_STRUCTURES);
    *///?} else {
    private static final Identifier ID_HANDSHAKE = Identifier.tryParse(HandshakeConstants.CHANNEL_ID);
    private static final Identifier ID_TARGETS = Identifier.tryParse(HandshakeConstants.CHANNEL_TARGETS);
    private static final Identifier ID_STRUCTURES = Identifier.tryParse(HandshakeConstants.CHANNEL_STRUCTURES);
    //?}

    @Override
    public void onInitializeClient() {
        ClientHandshakeManager manager = SongPicker.clientHandshakeManager;

        //? if >=1.20.5 {
        /*PayloadTypeRegistry.playC2S().register(HandshakeRequestPayload.TYPE, HandshakeRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HandshakeResponsePayload.TYPE, HandshakeResponsePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MobTargetPayload.TYPE, MobTargetPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StructurePayload.TYPE, StructurePayload.CODEC);

        manager.setRequestSender(version -> {
            if (!ClientPlayNetworking.canSend(HandshakeRequestPayload.TYPE)) return; // сервер без мода — не шлём
            ClientPlayNetworking.send(new HandshakeRequestPayload(version));
        });

        ClientPlayNetworking.registerGlobalReceiver(HandshakeResponsePayload.TYPE,
                (payload, context) -> SongPicker.clientHandshakeManager.onServerResponse(payload.serverVersion()));
        ClientPlayNetworking.registerGlobalReceiver(MobTargetPayload.TYPE,
                (payload, context) -> SongPicker.onMobTargetPacket(payload.mobUuid(), payload.status()));
        ClientPlayNetworking.registerGlobalReceiver(StructurePayload.TYPE,
                (payload, context) -> SongPicker.onStructurePacket(payload.structures()));
        *///?} else {
        manager.setRequestSender(version -> {
            if (!ClientPlayNetworking.canSend(ID_HANDSHAKE)) return; // сервер без мода — не шлём
            PacketByteBuf buf = PacketByteBufs.create();
            new HandshakePackets.HandshakeRequest(version).write(buf);
            ClientPlayNetworking.send(ID_HANDSHAKE, buf);
        });

        ClientPlayNetworking.registerGlobalReceiver(ID_HANDSHAKE,
                (client, handler, buf, responseSender) -> {
                    var response = HandshakePackets.HandshakeResponse.read(buf);
                    manager.onServerResponse(response.serverVersion);
                });

        ClientPlayNetworking.registerGlobalReceiver(ID_TARGETS,
                (client, handler, buf, responseSender) -> {
                    var packet = ServerDataPackets.MobTargetPacket.read(buf);
                    SongPicker.onMobTargetPacket(packet.mobUuid, packet.status);
                });

        ClientPlayNetworking.registerGlobalReceiver(ID_STRUCTURES,
                (client, handler, buf, responseSender) -> {
                    var packet = ServerDataPackets.StructurePacket.read(buf);
                    SongPicker.onStructurePacket(packet.structures);
                });
        //?}

        ClientPlayConnectionEvents.JOIN.register((handler, client, sentConnectPacket) -> {
            manager.onPlayerJoin(HandshakeConstants.MOD_VERSION);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            manager.onPlayerLeave();
            manager.clear();
        });
    }

    //? if >=1.20.5 {
    /*public record HandshakeRequestPayload(String clientVersion) implements CustomPayload {
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

    public record MobTargetPayload(UUID mobUuid, MobTargetStatus status) implements CustomPayload {
        public static final CustomPayload.Id<MobTargetPayload> TYPE =
                new CustomPayload.Id<>(Identifier.tryParse(HandshakeConstants.CHANNEL_TARGETS));
        public static final PacketCodec<RegistryByteBuf, MobTargetPayload> CODEC = PacketCodec.ofStatic(
                (RegistryByteBuf buf, MobTargetPayload v) -> {
                    buf.writeUuid(v.mobUuid());
                    buf.writeByte(v.status().ordinal());
                },
                buf -> new MobTargetPayload(buf.readUuid(), MobTargetStatus.byOrdinal(buf.readByte())));
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
    *///?}
}
