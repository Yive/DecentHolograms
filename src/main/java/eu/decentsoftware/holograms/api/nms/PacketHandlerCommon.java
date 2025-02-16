package eu.decentsoftware.holograms.api.nms;

import eu.decentsoftware.holograms.api.DecentHologramsAPI;
import eu.decentsoftware.holograms.api.actions.ClickType;
import lombok.experimental.UtilityClass;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.bukkit.entity.Player;

@UtilityClass
public final class PacketHandlerCommon {

    /**
     * Handle the PacketPlayInEntityUse packet and detect possible hologram clicks.
     *
     * @param packet The packet.
     * @param player The player that clicked.
     */
    public static boolean handlePacket(Object packet, Player player) {
        if (!(packet instanceof ServerboundInteractPacket interactPacket)) return false;
        int entityId = interactPacket.getEntityId();
        ClickType clickType = getClickType(interactPacket, player);
        return DecentHologramsAPI.get().getHologramManager().onClick(player, entityId, clickType);
    }

    private static ClickType getClickType(ServerboundInteractPacket packet, Player player) {
        if (packet.isAttack()) {
            return player.isSneaking() ? ClickType.SHIFT_LEFT : ClickType.LEFT;
        } else {
            return player.isSneaking() ? ClickType.SHIFT_RIGHT : ClickType.RIGHT;
        }
    }

}
