package circuitlord.reactivemusic.client.handshake;

import java.util.function.Consumer;

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

    /** Отправитель HandshakeRequest, устанавливается веткой лоадера. */
    private Consumer<String> requestSender = null;

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

    /** Очистить всё состояние (например, при выгрузке мира). */
    public void clear() {
        serverAcknowledged = false;
        serverVersion = null;
        lastRequestTime = 0L;
    }

    /** Установить отправитель HandshakeRequest (вызывается веткой лоадера). */
    public void setRequestSender(Consumer<String> sender) {
        this.requestSender = sender;
    }

    public void sendRequest(String clientVersion) {
        lastRequestTime = System.currentTimeMillis();
        if (requestSender != null) {
            requestSender.accept(clientVersion);
        } else {
            throw new UnsupportedOperationException("Loader-specific network send required");
        }
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
