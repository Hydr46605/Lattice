package dev.beryl.lattice.paper.ui;

import dev.beryl.lattice.ui.VirtualSignTextInputSurface;
import io.papermc.paper.math.Position;
import java.lang.reflect.Method;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;

final class PaperVirtualSignTextInputRenderer {
    boolean supported() {
        try {
            Method openVirtualSign = Player.class.getMethod("openVirtualSign", Position.class, Side.class);
            Class.forName("io.papermc.paper.event.packet.UncheckedSignChangeEvent");
            return openVirtualSign != null;
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            return false;
        }
    }

    boolean open(Player player, PaperTextInputUiSession session) {
        if (!(session.surface() instanceof VirtualSignTextInputSurface surface) || !supported()) {
            return false;
        }

        Location location;
        Position position;
        try {
            position = Position.block(player.getLocation());
            location = position.toLocation(player.getWorld());
            if (location == null) {
                return false;
            }
        } catch (IllegalStateException e) {
            return false;
        }

        session.signPosition(position.toBlock());
        session.signLocation(location);
        List<Component> lines = surface.initialLines().stream().<Component>map(Component::text).toList();
        player.sendBlockChange(location, Material.OAK_SIGN.createBlockData());
        player.sendSignChange(location, lines, DyeColor.BLACK, false);
        player.openVirtualSign(position, Side.FRONT);
        return true;
    }

    void restore(Player player, PaperTextInputUiSession session) {
        Location location = session.signLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }

        try {
            player.sendBlockChange(location, location.getBlock().getBlockData());
        } catch (IllegalStateException e) {
            // Thread doesn't own this region, skip restoration
            // Block state will update naturally on next interaction
        }
    }
}
