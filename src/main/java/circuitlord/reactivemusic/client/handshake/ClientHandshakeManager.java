package circuitlord.reactivemusic.client.handshake;

import java.util.function.Consumer;

/**
 * Клиентская часть рукопожатия с серверным модом ReactiveMusic-Server.
 * <p>
 * Отправляет HandshakeRequest при входе на сервер и ожидает HandshakeResponse.
 * Если ответа нет — повторяет запрос (см. {@link #tickRetry()}) ограниченное время,
 * затем работает в режиме только эвристики.
 * <p>
 * Сетевая отправка реализуется в каждой ветке лоадера (Fabric/Forge/NeoForge),
 * этот класс содержит только логику состояния.
 */
public class ClientHandshakeManager {

    /** Есть ли подтверждение от сервера, что у него есть серверный мод. */
    private volatile boolean serverAcknowledged = false;

    /** Время первого запроса в текущей сессии (для ограничения повторов). 0 = не входили. */
    private long firstRequestTime = 0L;

    /** Время отправки последнего HandshakeRequest (для интервала повторной отправки). */
    private long lastRequestTime = 0L;

    /** Версия клиента, сохранённая при входе — нужна для повторных запросов. */
    private String clientVersion = null;

    /** Серверная версия, если известна. */
    private volatile String serverVersion = null;

    /** Отправитель HandshakeRequest, устанавливается веткой лоадера. */
    private Consumer<String> requestSender = null;

    /** Сколько мс после первого запроса повторяем, прежде чем сдаться (сервер без мода). */
    private static final long RETRY_GIVEUP_MS = 60_000L;

    public void onPlayerJoin(String clientVersion) {
        serverAcknowledged = false;
        serverVersion = null;
        this.clientVersion = clientVersion;
        long now = System.currentTimeMillis();
        firstRequestTime = now;
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
        clientVersion = null;
        firstRequestTime = 0L;
        lastRequestTime = 0L;
    }

    /** Очистить всё состояние (например, при выгрузке мира). */
    public void clear() {
        serverAcknowledged = false;
        serverVersion = null;
        clientVersion = null;
        firstRequestTime = 0L;
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

    /**
     * Вызывать каждый клиентский тик. Повторяет HandshakeRequest, пока сервер не ответил,
     * но не дольше {@link #RETRY_GIVEUP_MS} с момента первого запроса — чтобы не спамить
     * серверы без мода бесконечно.
     */
    public void tickRetry() {
        if (serverAcknowledged) return;
        if (requestSender == null || clientVersion == null) return;
        if (firstRequestTime == 0L) return; // ещё не входили на сервер
        long now = System.currentTimeMillis();
        if (now - firstRequestTime > RETRY_GIVEUP_MS) return; // сдались — вероятно, сервер без мода
        if (now - lastRequestTime >= HandshakeConstants.HANDSHAKE_RETRY_MS) {
            sendRequest(clientVersion);
        }
    }

    public boolean isAcknowledged() {
        return serverAcknowledged;
    }

    public String getServerVersion() {
        return serverVersion;
    }
}
