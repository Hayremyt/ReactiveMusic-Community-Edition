package circuitlord.reactivemusic.neoforge.client;

import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.client.handshake.ClientHandshakeManager;
import circuitlord.reactivemusic.client.handshake.HandshakeConstants;
import circuitlord.reactivemusic.client.network.HandshakePackets;
import circuitlord.reactivemusic.client.network.ServerDataPackets;
import net.minecraft.network.PacketByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.network.NetworkEvent;
import net.neoforged.network.NetworkRegistry;
import net.neoforged.network.simple.SimpleChannel;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Клиентская сетевая интеграция с серверным модом для NeoForge.
 * <p>
 * Регистрирует приём пакетов рукопожатия, целей мобов и структур по сети.
 * Отправляет HandshakeRequest при входе на сервер.
 */
@EventBusSubscriber(modid = "reactivemusic", value = Dist.CLIENT)
public class NeoForgeClientHandshake {

    private static SimpleChannel handshakeChannel;
    private static SimpleChannel targetsChannel;
    private static SimpleChannel structuresChannel;

    public static void init(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientHandshakeManager manager = SongPicker.clientHandshakeManager;

            // --- Канал рукопожатия ---
            handshakeChannel = NetworkRegistry.ChannelBuilder
                    .named(HandshakeConstants.CHANNEL_ID)
                    .clientAcceptedVersions(s -> true)
                    .serverAcceptedVersions(s -> true)
                    .networkProtocolVersion(() -> "1")
                    .simpleChannel();

            handshakeChannel.registerMessage(0, HandshakePackets.HandshakeResponse.class,
                    NeoForgeClientHandshake::encodeResponse,
                    NeoForgeClientHandshake::decodeResponse,
                    NeoForgeClientHandshake::handleResponse);

            // Отправитель HandshakeRequest
            manager.setRequestSender(clientVersion ->
                    handshakeChannel.sendToServer(new HandshakePackets.HandshakeRequest(clientVersion)));

            // --- Канал целей мобов ---
            targetsChannel = NetworkRegistry.ChannelBuilder
                    .named(HandshakeConstants.CHANNEL_TARGETS)
                    .clientAcceptedVersions(s -> true)
                    .serverAcceptedVersions(s -> true)
                    .networkProtocolVersion(() -> "1")
                    .simpleChannel();

            targetsChannel.registerMessage(0, ServerDataPackets.MobTargetPacket.class,
                    NeoForgeClientHandshake::encodeMobTarget,
                    NeoForgeClientHandshake::decodeMobTarget,
                    NeoForgeClientHandshake::handleMobTarget);

            // --- Канал структур ---
            structuresChannel = NetworkRegistry.ChannelBuilder
                    .named(HandshakeConstants.CHANNEL_STRUCTURES)
                    .clientAcceptedVersions(s -> true)
                    .serverAcceptedVersions(s -> true)
                    .networkProtocolVersion(() -> "1")
                    .simpleChannel();

            structuresChannel.registerMessage(0, ServerDataPackets.StructurePacket.class,
                    NeoForgeClientHandshake::encodeStructure,
                    NeoForgeClientHandshake::decodeStructure,
                    NeoForgeClientHandshake::handleStructure);
        });
    }

    @SubscribeEvent
    public static void onClientLoggedIn(PlayerEvent.ClientLoggedInEvent event) {
        SongPicker.clientHandshakeManager.onPlayerJoin("1.3.5");
    }

    @SubscribeEvent
    public static void onClientLoggedOut(PlayerEvent.ClientLoggedOutEvent event) {
        SongPicker.clientHandshakeManager.onPlayerLeave();
        SongPicker.clientHandshakeManager.clear();
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
}
