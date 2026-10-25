package circuitlord.reactivemusic.client.network;

import java.util.UUID;

import net.minecraft.network.PacketByteBuf;

/**
 * Клиентские копии пакетов данных от серверного мода.
 * <p>
 * Определены здесь, а не в серверномlere интеграции. Формат кодирования должен совпадать
 * с серверной стороной ({@code ReactiveMusic-Server}).
 */
public final class ServerDataPackets {

    /** Сервер → Клиент: цель одного моба. */
    public static class MobTargetPacket {
        public final UUID mobUuid;
        public final UUID targetUuid; // null если цели нет

        public MobTargetPacket(UUID mobUuid, UUID targetUuid) {
            this.mobUuid = mobUuid;
            this.targetUuid = targetUuid;
        }

        public void write(PacketByteBuf buf) {
            buf.writeUuid(mobUuid);
            if (targetUuid == null) {
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(true);
                buf.writeUuid(targetUuid);
            }
        }

        public static MobTargetPacket read(PacketByteBuf buf) {
            UUID mobUuid = buf.readUuid();
            boolean hasTarget = buf.readBoolean();
            UUID targetUuid = hasTarget ? buf.readUuid() : null;
            return new MobTargetPacket(mobUuid, targetUuid);
        }
    }

    /** Сервер → Клиент: список структур вокруг игрока. */
    public static class StructurePacket {
        public final String[] structures;

        public StructurePacket(String[] structures) {
            this.structures = structures;
        }

        public void write(PacketByteBuf buf) {
            buf.writeInt(structures.length);
            for (var s : structures) {
                buf.writeString(s);
            }
        }

        public static StructurePacket read(PacketByteBuf buf) {
            int len = buf.readInt();
            var arr = new String[len];
            for (int i = 0; i < len; i++) {
                arr[i] = buf.readString();
            }
            return new StructurePacket(arr);
        }
    }

    private ServerDataPackets() {}
}
