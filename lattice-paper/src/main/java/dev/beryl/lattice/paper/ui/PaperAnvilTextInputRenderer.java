package dev.beryl.lattice.paper.ui;

import dev.beryl.lattice.ui.AnvilTextInputSurface;
import dev.beryl.lattice.util.Preconditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.view.AnvilView;

final class PaperAnvilTextInputRenderer {
    private static final int INPUT_SLOT = 0;
    private static final int RESULT_SLOT = 2;

    private final PaperUiIconRenderer icons;
    private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();

    PaperAnvilTextInputRenderer(PaperUiIconRenderer icons) {
        this.icons = Preconditions.requireNonNull(icons, "icons");
    }

    boolean open(Player player, PaperTextInputUiSession session) {
        if (!(session.surface() instanceof AnvilTextInputSurface surface)) {
            return false;
        }

        Location location;
        try {
            location = player.getLocation();
            if (location == null) {
                return false;
            }
        } catch (IllegalStateException e) {
            return false;
        }

        InventoryView view = player.openAnvil(location, true);
        if (!(view instanceof AnvilView anvilView)) {
            return false;
        }
        session.anvilView(view);
        view.setTitle(plainText.serialize(surface.title()));
        anvilView.setRepairCost(0);
        anvilView.setMaximumRepairCost(0);
        view.setItem(INPUT_SLOT, icons.item(surface.inputIcon().name(Component.text(surface.initialValue()))));
        view.setItem(RESULT_SLOT, icons.item(surface.resultIcon()));
        return true;
    }

    void refresh(PaperTextInputUiSession session) {
        if (!(session.surface() instanceof AnvilTextInputSurface surface) || session.anvilView() == null) {
            return;
        }
        AnvilView view = session.anvilView();
        view.setRepairCost(0);
        view.setMaximumRepairCost(0);
        view.setItem(RESULT_SLOT, icons.item(surface.resultIcon()));
    }
}
