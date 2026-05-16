package dev.beryl.lattice.ui;

import dev.beryl.lattice.util.Preconditions;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;

public record BookViewSurface(
        String id,
        Component title,
        Component author,
        List<Component> pages
) implements UiSurface {
    public BookViewSurface {
        id = Preconditions.requireText(id, "id");
        title = Preconditions.requireNonNull(title, "title");
        author = author == null ? Component.text("Lattice") : author;
        pages = List.copyOf(pages == null ? List.of() : pages);
        Preconditions.checkArgument(!pages.isEmpty(), "Book view must have at least one page");
        Preconditions.checkArgument(pages.size() <= 100, "Book view cannot have more than 100 pages");
    }

    public static Builder book(String id, Component title) {
        return new Builder(id, title);
    }

    @Override
    public UiSurfaceType type() {
        return UiSurfaceType.BOOK_VIEW;
    }

    public static final class Builder {
        private final String id;
        private final Component title;
        private Component author;
        private final List<Component> pages = new ArrayList<>();

        private Builder(String id, Component title) {
            this.id = Preconditions.requireText(id, "id");
            this.title = Preconditions.requireNonNull(title, "title");
        }

        public Builder author(Component author) {
            this.author = Preconditions.requireNonNull(author, "author");
            return this;
        }

        public Builder page(Component page) {
            pages.add(Preconditions.requireNonNull(page, "page"));
            return this;
        }

        public BookViewSurface build() {
            return new BookViewSurface(id, title, author, pages);
        }
    }
}
