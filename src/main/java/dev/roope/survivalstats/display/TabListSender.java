package dev.roope.survivalstats.display;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TabListSender {
    private final Map<UUID, String> lastPayloadByPlayer = new HashMap<>();

    public void sendIfChanged(ServerPlayer player, RenderedTabList rendered) {
        UUID id = player.getUUID();
        String previous = lastPayloadByPlayer.get(id);
        if (rendered.cacheKey().equals(previous)) {
            return;
        }
        player.connection.send(new ClientboundTabListPacket(rendered.header(), rendered.footer()));
        lastPayloadByPlayer.put(id, rendered.cacheKey());
    }

    public void clear(ServerPlayer player) {
        player.connection.send(new ClientboundTabListPacket(Component.empty(), Component.empty()));
        lastPayloadByPlayer.remove(player.getUUID());
    }

    public void forget(ServerPlayer player) {
        lastPayloadByPlayer.remove(player.getUUID());
    }

    public void resetCache() {
        lastPayloadByPlayer.clear();
    }
}
