package circuitlord.reactivemusic.client.handshake;

/**
 * Клиентская часть рукопожатия с серверным модом ReactiveMusic-Server.
 * <p>
 * Отправляет HandshakeRequest при входе на сервер и ожидает HandshakeResponse.
 * Если ответа нет в течение таймаута, клиент работает в режиме только эвристики
 * (без точных данных от сервера).
 * <p>
 * Сетевая отправка реализуется в каждой ветке лоадера (Fabric/Forge/NeoForge),
 * этот класс содержит только логику состояния.
 */
public class ClientHandshakeManager {

    /** Есть ли подтверждение от сервера, что у него есть серверный мод. */
    private boolean serverAcknowledged = false;

    /** Время отправки последнего HandshakeRequest (для повторной отправки). */
    private long lastRequestTime = 0L;

    /** Серверная версия, если известна. */
    private String serverVersion = null;

    public void onPlayerJoin(String clientVersion) {
        serverAcknowledged = false;
        serverVersion = null;
        lastRequestTime = 0L;
        sendRequest(clientVersion);
    }

    public void onServerResponse(String serverVersion) {
        this.serverAcknowledged = true;
        this.serverVersion = serverVersion;
    }

    public void onPlayerLeave() {
        serverAcknowledged = false;
        serverVersion = null;
        lastRequestTime = 0L;
    }

    public void sendRequest(String clientVersion) {
        lastRequestTime = System.currentTimeMillis();
        // Сетевая отправка реализуется в ветке лоадера:
        //   Fabric: ClientPlayNetworking.send(...)
        //   Forge/NeoForge: SimpleChannel.sendToServer(...)
        throw new UnsupportedOperationException("Loader-specific network send required");
    }

    public boolean isAcknowledged() {
        return serverAcknowledged;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public boolean shouldRetry(long nowMs) {
        return !serverAcknowledged
                && (lastRequestTime == 0L || nowMs - lastRequestTime >= HandshakeConstants.HANDSHAKE_RETRY_MS);
    }

    public boolean isTimedOut(long nowMs) {
        return !serverAcknowledged
                && lastRequestTime != 0L
                && nowMs - lastRequestTime >= HandshakeConstants.HANDSHAKE_TIMEOUT_MS;
    }
}
