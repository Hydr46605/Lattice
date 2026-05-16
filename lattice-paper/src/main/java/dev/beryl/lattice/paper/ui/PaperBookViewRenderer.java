package dev.beryl.lattice.paper.ui;

import dev.beryl.lattice.ui.BookViewSurface;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

final class PaperBookViewRenderer {
    void open(Player player, PaperBookUiSession session) {
        BookViewSurface surface = session.surface();
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        if (book.getItemMeta() instanceof BookMeta meta) {
            meta.title(surface.title());
            meta.author(surface.author());
            meta.addPages(surface.pages().toArray(new net.kyori.adventure.text.Component[0]));
            book.setItemMeta(meta);
        }
        player.openBook(book);
    }
}
