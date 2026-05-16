package dev.beryl.lattice.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.beryl.lattice.module.ModuleId;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class UiScreenTest {
    @Test
    void buildsPagedScreenAndFindsButtons() {
        UiScreen screen = UiScreen.screen("shop", Component.text("Shop"))
                .rows(3)
                .page(UiPage.page("first")
                        .button(UiButton.display(10, UiIcon.material("diamond").name(Component.text("Buy"))))
                        .button(UiActions.nextPageButton(26, UiIcon.material("arrow")))
                        .build())
                .page(UiPage.page("second")
                        .button(UiActions.previousPageButton(18, UiIcon.material("arrow")))
                        .build())
                .build();

        assertEquals(27, screen.size());
        assertEquals(2, screen.pages().size());
        assertEquals("first", screen.page(0).id());
        assertTrue(screen.page(0).buttonAt(10).isPresent());
        assertEquals(1, screen.pageIndex("second").orElseThrow());
    }

    @Test
    void rejectsButtonsOutsideInventorySize() {
        assertThrows(IllegalArgumentException.class, () -> UiScreen.screen("bad", Component.text("Bad"))
                .rows(1)
                .page(UiPage.page("main")
                        .button(UiButton.display(9, UiIcon.material("stone")))
                        .build())
                .build());
    }

    @Test
    void rejectsDuplicatePageIdsAndSlots() {
        assertThrows(IllegalArgumentException.class, () -> UiPage.page("main")
                .button(UiButton.display(0, UiIcon.material("stone")))
                .button(UiButton.display(0, UiIcon.material("paper")))
                .build());

        UiPage page = UiPage.page("main").button(UiButton.display(0, UiIcon.material("stone"))).build();
        assertThrows(IllegalArgumentException.class, () -> UiScreen.screen("bad", Component.text("Bad"))
                .page(page)
                .page(page)
                .build());
    }

    @Test
    void detectsNexoIconsThroughFallbacks() {
        UiIcon icon = UiIcon.nexo("menu_next").fallback(UiIcon.material("arrow"));

        assertTrue(icon.uses(UiIconSource.CUSTOM));
        assertTrue(icon.usesCustomProvider("nexo"));
        assertTrue(icon.uses(UiIconSource.NEXO));
        assertTrue(icon.uses(UiIconSource.MATERIAL));
        assertFalse(UiIcon.material("stone").usesCustomProvider("nexo"));
    }

    @Test
    void buildsTypedSurfaces() {
        BookViewSurface book = BookViewSurface.book("guide", Component.text("Guide"))
                .author(Component.text("Docs"))
                .page(Component.text("First page"))
                .build();
        AnvilTextInputSurface anvil = AnvilTextInputSurface.input("rename", Component.text("Rename"))
                .initialValue("Start")
                .build();
        VirtualSignTextInputSurface sign = VirtualSignTextInputSurface.input("lines", Component.text("Lines"))
                .line("one")
                .line("two")
                .build();

        assertEquals(UiSurfaceType.BOOK_VIEW, book.type());
        assertEquals(1, book.pages().size());
        assertEquals(UiSurfaceType.ANVIL_TEXT_INPUT, anvil.type());
        assertEquals(List.of("Start"), anvil.initialLines());
        assertEquals(UiSurfaceType.VIRTUAL_SIGN_TEXT_INPUT, sign.type());
        assertEquals(List.of("one", "two", "", ""), sign.initialLines());
    }

    @Test
    void navigationActionsUpdateSessionPage() throws Exception {
        RecordingSession session = new RecordingSession(UiScreen.screen("menu", Component.text("Menu"))
                .page(UiPage.page("one").build())
                .page(UiPage.page("two").build())
                .build());
        UiButton button = UiButton.of(0, UiIcon.material("arrow"), UiActions.nextPage());
        UiClick click = new UiClick(
                session,
                UiViewerRef.player(UUID.randomUUID(), "Hydra", "world"),
                session.screen(),
                session.page(),
                button,
                0,
                UiClickType.LEFT
        );

        button.handler().handle(click);

        assertEquals(1, session.pageIndex());
        assertEquals("two", session.page().id());
    }

    private static final class RecordingSession implements InventoryUiSession {
        private final UUID id = UUID.randomUUID();
        private final UiOwner owner = new UiOwner("test", ModuleId.of("ui-test"));
        private final UiViewerRef viewer = UiViewerRef.player(UUID.randomUUID(), "Hydra", "world");
        private final UiScreen screen;
        private int pageIndex;
        private boolean closed;

        private RecordingSession(UiScreen screen) {
            this.screen = screen;
        }

        @Override
        public UUID id() {
            return id;
        }

        @Override
        public UiOwner owner() {
            return owner;
        }

        @Override
        public UiViewerRef viewer() {
            return viewer;
        }

        @Override
        public UiScreen surface() {
            return screen;
        }

        @Override
        public UiScreen screen() {
            return screen;
        }

        @Override
        public int pageIndex() {
            return pageIndex;
        }

        @Override
        public UiPage page() {
            return screen.page(pageIndex);
        }

        @Override
        public void openPage(int pageIndex) {
            screen.page(pageIndex);
            this.pageIndex = pageIndex;
        }

        @Override
        public void openPage(String pageId) {
            openPage(screen.pageIndex(pageId).orElseThrow());
        }

        @Override
        public void nextPage() {
            if (pageIndex + 1 < screen.pages().size()) {
                openPage(pageIndex + 1);
            }
        }

        @Override
        public void previousPage() {
            if (pageIndex > 0) {
                openPage(pageIndex - 1);
            }
        }

        @Override
        public void refresh() {
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public boolean closed() {
            return closed;
        }
    }
}
