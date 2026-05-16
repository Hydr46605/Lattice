package dev.beryl.lattice.ui;

import dev.beryl.lattice.util.Preconditions;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.text.Component;

public record UiScreen(String id, Component title, int size, List<UiPage> pages) implements UiSurface {
    public UiScreen {
        id = Preconditions.requireText(id, "id");
        title = Preconditions.requireNonNull(title, "title");
        Preconditions.checkArgument(size >= 9 && size <= 54 && size % 9 == 0, "UI size must be a multiple of 9 from 9 to 54");
        pages = List.copyOf(pages == null ? List.of() : pages);
        Preconditions.checkArgument(!pages.isEmpty(), "UI screen must have at least one page");
        validatePages(size, pages);
    }

    public static Builder screen(String id, Component title) {
        return new Builder(id, title);
    }

    @Override
    public UiSurfaceType type() {
        return UiSurfaceType.INVENTORY;
    }

    public int rows() {
        return size / 9;
    }

    public UiPage page(int index) {
        Preconditions.checkArgument(index >= 0 && index < pages.size(), "UI page index out of bounds: " + index);
        return pages.get(index);
    }

    public Optional<Integer> pageIndex(String pageId) {
        for (int index = 0; index < pages.size(); index++) {
            if (pages.get(index).id().equals(pageId)) {
                return Optional.of(index);
            }
        }
        return Optional.empty();
    }

    public boolean uses(UiIconSource source) {
        return pages.stream().anyMatch(page -> page.uses(source));
    }

    public boolean usesCustomProvider(String providerId) {
        return pages.stream().anyMatch(page -> page.usesCustomProvider(providerId));
    }

    private static void validatePages(int size, List<UiPage> pages) {
        Set<String> ids = new LinkedHashSet<>();
        for (UiPage page : pages) {
            if (!ids.add(page.id())) {
                throw new IllegalArgumentException("Duplicate UI page id: " + page.id());
            }
            for (UiButton button : page.buttons()) {
                Preconditions.checkArgument(button.slot() < size, "UI button slot " + button.slot() + " is outside size " + size);
            }
        }
    }

    public static final class Builder {
        private final String id;
        private final Component title;
        private int rows = 6;
        private final List<UiPage> pages = new ArrayList<>();

        private Builder(String id, Component title) {
            this.id = Preconditions.requireText(id, "id");
            this.title = Preconditions.requireNonNull(title, "title");
        }

        public Builder rows(int rows) {
            Preconditions.checkArgument(rows >= 1 && rows <= 6, "UI rows must be from 1 to 6");
            this.rows = rows;
            return this;
        }

        public Builder page(UiPage page) {
            pages.add(Preconditions.requireNonNull(page, "page"));
            return this;
        }

        public UiScreen build() {
            return new UiScreen(id, title, rows * 9, pages);
        }
    }
}
