package circuitlord.reactivemusic.forge.client;

import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.client.handshake.ClientHandshakeManager;
import circuitlord.reactivemusic.client.handshake.HandshakeConstants;
import circuitlord.reactivemusic.client.network.HandshakePackets;
import circuitlord.reactivemusic.client.network.ServerDataPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * Клиентская сетевая интеграция с серверным модом для Forge.
 * <p>
 * Каналы и порядок сообщений совпадают с серверным модом (request@0, response@1 на канале
 * рукопожатия; targets@0; structures@0), иначе Forge SimpleChannel не сможет декодировать.
 */
@EventBusSubscriber(modid = "reactivemusic", value = Dist.CLIENT)
public class ForgeClientHandshake {

    private static SimpleChannel handshakeChannel;
    private static SimpleChannel targetsChannel;
    private static SimpleChannel structuresChannel;

    public static void init(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientHandshakeManager manager = SongPicker.clientHandshakeManager;

            // --- Канал рукопожатия (request@0, response@1 — как на сервере) ---
            handshakeChannel = NetworkRegistry.ChannelBuilder
                    .named(Identifier.tryParse(HandshakeConstants.CHANNEL_ID))
                    .networkProtocolVersion(() -> "1")
                    .clientAcceptedVersions(s -> true)
                    .serverAcceptedVersions(s -> true)
                    .simpleChannel();

            handshakeChannel.registerMessage(0, HandshakePackets.HandshakeRequest.class,
                    ForgeClientHandshake::encodeRequest,
                    ForgeClientHandshake::decodeRequest,
                    ForgeClientHandshake::handleNoop);

            handshakeChannel.registerMessage(1, HandshakePackets.HandshakeResponse.class,
                    ForgeClientHandshake::encodeResponse,
                    ForgeClientHandshake::decodeResponse,
                    ForgeClientHandshake::handleResponse);

            manager.setRequestSender(clientVersion ->
                    handshakeChannel.sendToServer(new HandshakePackets.HandshakeRequest(clientVersion)));

            // --- Канал целей мобов ---
            targetsChannel = NetworkRegistry.ChannelBuilder
                    .named(Identifier.tryParse(HandshakeConstants.CHANNEL_TARGETS))
                    .networkProtocolVersion(() -> "1")
                    .clientAcceptedVersions(s -> true)
                    .serverAcceptedVersions(s -> true)
                    .simpleChannel();

            targetsChannel.registerMessage(0, ServerDataPackets.MobTargetPacket.class,
                    ForgeClientHandshake::encodeMobTarget,
                    ForgeClientHandshake::decodeMobTarget,
                    ForgeClientHandshake::handleMobTarget);

            // --- Канал структур ---
            structuresChannel = NetworkRegistry.ChannelBuilder
                    .named(Identifier.tryParse(HandshakeConstants.CHANNEL_STRUCTURES))
                    .networkProtocolVersion(() -> "1")
                    .clientAcceptedVersions(s -> true)
                    .serverAcceptedVersions(s -> true)
                    .simpleChannel();

            structuresChannel.registerMessage(0, ServerDataPackets.StructurePacket.class,
                    ForgeClientHandshake::encodeStructure,
                    ForgeClientHandshake::decodeStructure,
                    ForgeClientHandshake::handleStructure);
        });
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

    // ---------- HandshakeRequest ----------

    private static void encodeRequest(HandshakePackets.HandshakeRequest msg, PacketByteBuf buf) {
        msg.write(buf);
    }

    private static HandshakePackets.HandshakeRequest decodeRequest(PacketByteBuf buf) {
        return HandshakePackets.HandshakeRequest.read(buf);
    }

    // ---------- HandshakeResponse ----------

    private static void encodeResponse(HandshakePackets.HandshakeResponse msg, PacketByteBuf buf) {
        msg.write(buf);
    }

    private static HandshakePackets.HandshakeResponse decodeResponse(PacketByteBuf buf) {
        return HandshakePackets.HandshakeResponse.read(buf);
    }

    private static void handleResponse(HandshakePackets.HandshakeResponse msg,
                                       Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> SongPicker.clientHandshakeManager.onServerResponse(msg.serverVersion));
        ctx.get().setPacketHandled(true);
    }

    // ---------- MobTargetPacket ----------

    private static void encodeMobTarget(ServerDataPackets.MobTargetPacket msg, PacketByteBuf buf) {
        msg.write(buf);
    }

    private static ServerDataPackets.MobTargetPacket decodeMobTarget(PacketByteBuf buf) {
        return ServerDataPackets.MobTargetPacket.read(buf);
    }

    private static void handleMobTarget(ServerDataPackets.MobTargetPacket msg,
                                        Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> SongPicker.onMobTargetPacket(msg.mobUuid, msg.targetUuid));
        ctx.get().setPacketHandled(true);
    }

    // ---------- StructurePacket ----------

    private static void encodeStructure(ServerDataPackets.StructurePacket msg, PacketByteBuf buf) {
        msg.write(buf);
    }

    private static ServerDataPackets.StructurePacket decodeStructure(PacketByteBuf buf) {
        return ServerDataPackets.StructurePacket.read(buf);
    }

    private static void handleStructure(ServerDataPackets.StructurePacket msg,
                                        Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> SongPicker.onStructurePacket(msg.structures));
        ctx.get().setPacketHandled(true);
    }

    private static void handleNoop(Object msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
    }
}
