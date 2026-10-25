package circuitlord.reactivemusic.fabric.client;

import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.client.handshake.ClientHandshakeManager;
import circuitlord.reactivemusic.client.handshake.HandshakeConstants;
import circuitlord.reactivemusic.client.network.HandshakePackets;
import circuitlord.reactivemusic.client.network.ServerDataPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketByteBufs;

/**
 * Клиентская сетевая интеграция с серверным модом для Fabric.
 * <p>
 * Регистрирует приём пакетов рукопожатия, целей мобов и структур по сети.
 * Отправляет HandshakeRequest при входе на сервер.
 */
public class FabricClientHandshake implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientHandshakeManager manager = SongPicker.clientHandshakeManager;

        // --- Отправитель HandshakeRequest (клиент -> сервер) ---
        manager.setRequestSender(clientVersion -> {
            PacketByteBuf buf = new PacketByteBuf(PacketByteBufs.empty());
            new HandshakePackets.HandshakeRequest(clientVersion).write(buf);
            ClientPlayNetworking.send(HandshakeConstants.CHANNEL_ID, buf);
        });

        // --- Приём HandshakeResponse (сервер -> клиент) ---
        ClientPlayNetworking.registerReceiver(
                HandshakeConstants.CHANNEL_ID,
                (client, handler, buf, responseSender) -> {
                    var response = HandshakePackets.HandshakeResponse.read(buf);
                    manager.onServerResponse(response.serverVersion);
                }
        );

        // --- Приём MobTargetPacket (сервер -> клиент) ---
        ClientPlayNetworking.registerReceiver(
                HandshakeConstants.CHANNEL_TARGETS,
                (client, handler, buf, responseSender) -> {
                    var packet = ServerDataPackets.MobTargetPacket.read(buf);
                    SongPicker.onMobTargetPacket(packet.mobUuid, packet.targetUuid);
                }
        );

        // --- Приём StructurePacket (сервер -> клиент) ---
        ClientPlayNetworking.registerReceiver(
                HandshakeConstants.CHANNEL_STRUCTURES,
                (client, handler, buf, responseSender) -> {
                    var packet = ServerDataPackets.StructurePacket.read(buf);
                    SongPicker.onStructurePacket(packet.structures);
                }
        );

        // --- Вход на сервер: инициируем рукопожатие ---
        ClientPlayConnectionEvents.JOIN.register((handler, client, sentConnectPacket) -> {
            manager.onPlayerJoin("1.3.5");
        });

        // --- Выход с сервера: сбрасываем состояние ---
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            manager.onPlayerLeave();
            manager.clear();
        });
    }
}
