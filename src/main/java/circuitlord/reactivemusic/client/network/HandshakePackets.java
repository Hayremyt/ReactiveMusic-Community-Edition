package circuitlord.reactivemusic.client.network;

import net.minecraft.network.PacketByteBuf;

/**
 * Клиентские копии пакетов рукопожатия.
 * <p>
 * Определены здесь, а не в серверном моде, чтобы основной мод не зависел от
 * {@code ReactiveMusic-Server} (он опциональный, отдельный репозиторий).
 * Формат кодирования должен совпадать с серверной стороной.
 */
public final class HandshakePackets {

    /** Клиент → Сервер: "у меня установлен клиентский мод ReactiveMusic". */
    public static class HandshakeRequest {
        public final String clientVersion;

        public HandshakeRequest(String clientVersion) {
            this.clientVersion = clientVersion;
        }

        public void write(PacketByteBuf buf) {
            buf.writeString(clientVersion);
        }

        public static HandshakeRequest read(PacketByteBuf buf) {
            return new HandshakeRequest(buf.readString());
        }
    }

    /** Сервер → Клиент: "у меня установлен серверный мод ReactiveMusic". */
    public static class HandshakeResponse {
        public final String serverVersion;

        public HandshakeResponse(String serverVersion) {
            this.serverVersion = serverVersion;
        }

        public void write(PacketByteBuf buf) {
            buf.writeString(serverVersion);
        }

        public static HandshakeResponse read(PacketByteBuf buf) {
            return new HandshakeResponse(buf.readString());
        }
    }

    private HandshakePackets() {}
}
