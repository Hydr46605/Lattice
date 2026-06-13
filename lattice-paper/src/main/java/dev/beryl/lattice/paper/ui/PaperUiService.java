package dev.beryl.lattice.paper.ui;

import dev.beryl.lattice.diagnostics.UiDiagnostics;
import dev.beryl.lattice.integration.IntegrationManager;
import dev.beryl.lattice.paper.integration.CustomItemRegistry;
import dev.beryl.lattice.task.EntityRef;
import dev.beryl.lattice.task.TaskContext;
import dev.beryl.lattice.task.TaskOwner;
import dev.beryl.lattice.task.TaskSchedule;
import dev.beryl.lattice.task.TaskService;
import dev.beryl.lattice.ui.AnvilTextInputSurface;
import dev.beryl.lattice.ui.BookViewSurface;
import dev.beryl.lattice.ui.TextInputSurface;
import dev.beryl.lattice.ui.UiButton;
import dev.beryl.lattice.ui.UiClick;
import dev.beryl.lattice.ui.UiClickType;
import dev.beryl.lattice.ui.UiException;
import dev.beryl.lattice.ui.UiIcon;
import dev.beryl.lattice.ui.UiOpenFailureReason;
import dev.beryl.lattice.ui.UiOpenResult;
import dev.beryl.lattice.ui.UiOwner;
import dev.beryl.lattice.ui.UiScreen;
import dev.beryl.lattice.ui.UiService;
import dev.beryl.lattice.ui.UiSession;
import dev.beryl.lattice.ui.UiSurface;
import dev.beryl.lattice.ui.UiSurfaceType;
import dev.beryl.lattice.ui.UiTextInput;
import dev.beryl.lattice.ui.UiUnsupportedSurfaceException;
import dev.beryl.lattice.ui.UiViewerRef;
import dev.beryl.lattice.ui.VirtualSignTextInputSurface;
import dev.beryl.lattice.util.Preconditions;
import io.papermc.paper.math.BlockPosition;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperUiService implements UiService, Listener {
    private static final int ANVIL_RESULT_SLOT = 2;

    private final JavaPlugin plugin;
    private final TaskService tasks;
    private final Map<UUID, PaperUiSession> sessionsByViewer = new LinkedHashMap<>();
    private final Map<UUID, PaperUiSession> sessionsById = new LinkedHashMap<>();
    private final Set<String> registeredRefreshEvents = new LinkedHashSet<>();
    private final PaperUiIconRenderer iconRenderer;
    private final PaperInventorySurfaceRenderer inventoryRenderer;
    private final PaperBookViewRenderer bookRenderer = new PaperBookViewRenderer();
    private final PaperAnvilTextInputRenderer anvilRenderer;
    private final PaperVirtualSignTextInputRenderer virtualSignRenderer = new PaperVirtualSignTextInputRenderer();
    private final ReentrantLock lock = new ReentrantLock();
    private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();
    private boolean eventHandlersRegistered;

    public PaperUiService(JavaPlugin plugin, TaskService tasks, IntegrationManager integrations) {
        this.plugin = Preconditions.requireNonNull(plugin, "plugin");
        this.tasks = Preconditions.requireNonNull(tasks, "tasks");
        Preconditions.requireNonNull(integrations, "integrations");
        this.iconRenderer = new PaperUiIconRenderer(integrations);
        this.inventoryRenderer = new PaperInventorySurfaceRenderer(iconRenderer);
        this.anvilRenderer = new PaperAnvilTextInputRenderer(iconRenderer);
        registerEventHandlersIfEnabled();
    }

    @Override
    public UiSession open(UiOwner owner, UiViewerRef viewer, UiSurface surface) {
        UiOpenResult result = tryOpen(owner, viewer, surface);
        if (result.opened()) {
            return result.session();
        }
        if (result.failureReason() == UiOpenFailureReason.UNSUPPORTED_SURFACE) {
            throw new UiUnsupportedSurfaceException(surface, result.message());
        }
        if (result.failureReason() == UiOpenFailureReason.OFFLINE_VIEWER) {
            throw new IllegalStateException(result.message());
        }
        throw new UiException(result.message());
    }

    @Override
    public UiOpenResult tryOpen(UiOwner owner, UiViewerRef viewer, UiSurface surface) {
        Preconditions.requireNonNull(owner, "owner");
        Preconditions.requireNonNull(viewer, "viewer");
        Preconditions.requireNonNull(surface, "surface");
        registerEventHandlersIfEnabled();

        Optional<Player> player = player(viewer);
        if (player.isEmpty()) {
            return UiOpenResult.failed(
                    UiOpenFailureReason.OFFLINE_VIEWER,
                    "Cannot open UI for offline player " + viewer.playerId()
            );
        }

        PaperUiSession session;
        if (surface instanceof UiScreen screen) {
            session = new PaperInventoryUiSession(this, owner, viewer, screen);
        } else if (surface instanceof BookViewSurface book) {
            session = new PaperBookUiSession(this, owner, viewer, book);
        } else if (surface instanceof AnvilTextInputSurface input) {
            session = new PaperTextInputUiSession(this, owner, viewer, input);
        } else if (surface instanceof VirtualSignTextInputSurface input) {
            if (!virtualSignRenderer.supported()) {
                return UiOpenResult.failed(
                        UiOpenFailureReason.UNSUPPORTED_SURFACE,
                        "Virtual sign input is not available on this Paper runtime"
                );
            }
            session = new PaperTextInputUiSession(this, owner, viewer, input);
        } else {
            return UiOpenResult.failed(
                    UiOpenFailureReason.UNSUPPORTED_SURFACE,
                    "No Paper renderer registered for " + surface.type()
            );
        }

        runOnPlayer(owner, player.get(), () -> openNow(player.get(), session));
        return UiOpenResult.opened(session);
    }

    @Override
    public synchronized Optional<UiSession> session(UiViewerRef viewer) {
        Preconditions.requireNonNull(viewer, "viewer");
        return Optional.ofNullable(sessionsByViewer.get(viewer.playerId()));
    }

    @Override
    public void close(UiViewerRef viewer) {
        Preconditions.requireNonNull(viewer, "viewer");
        PaperUiSession session;
        synchronized (this) {
            session = sessionsByViewer.get(viewer.playerId());
        }
        if (session == null) {
            return;
        }

        Optional<Player> player = player(viewer);
        if (player.isEmpty()) {
            remove(session);
            return;
        }
        runOnPlayer(session.owner(), player.get(), () -> closeNow(player.get(), session));
    }

    @Override
    public void closeAll() {
        List<PaperUiSession> sessions;
        synchronized (this) {
            sessions = new ArrayList<>(sessionsById.values());
        }
        for (PaperUiSession session : sessions) {
            Optional<Player> player = player(session.viewer());
            if (player.isPresent()) {
                runOnPlayer(session.owner(), player.get(), () -> closeNow(player.get(), session));
            } else {
                remove(session);
            }
        }
    }

    @Override
    public synchronized UiDiagnostics diagnostics() {
        Map<UiSurfaceType, Integer> bySurface = new EnumMap<>(UiSurfaceType.class);
        for (PaperUiSession session : sessionsById.values()) {
            bySurface.merge(session.surface().type(), 1, Integer::sum);
        }
        return new UiDiagnostics(sessionsById.size(), bySurface);
    }

    @Override
    public void close() {
        closeAll();
        if (eventHandlersRegistered) {
            HandlerList.unregisterAll(this);
            eventHandlersRegistered = false;
        }
    }

    void refresh(PaperUiSession session) {
        if (session.closed()) {
            return;
        }
        Optional<Player> player = player(session.viewer());
        if (player.isEmpty()) {
            remove(session);
            return;
        }
        runOnPlayer(session.owner(), player.get(), () -> refreshNow(player.get(), session));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        PaperInventoryUiSession inventorySession = session(event.getView().getTopInventory()).orElse(null);
        if (inventorySession != null) {
            lock.lock();
            try {
                if (!inventorySession.closed()) {
                    handleInventoryClick(event, inventorySession);
                }
            } finally {
                lock.unlock();
            }
            return;
        }

        PaperTextInputUiSession inputSession = anvilSession(event).orElse(null);
        if (inputSession == null) {
            return;
        }

        event.setCancelled(true);
        if (event.getRawSlot() == ANVIL_RESULT_SLOT && event.getWhoClicked() instanceof Player player) {
            submitAnvilInput(player, inputSession);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        lock.lock();
        try {
            PaperTextInputUiSession session = anvilSession(event.getView().getPlayer().getUniqueId()).orElse(null);
            if (session == null || session.closed() || !(session.surface() instanceof AnvilTextInputSurface surface)) {
                return;
            }
            event.getView().setRepairCost(0);
            event.getView().setMaximumRepairCost(0);
            event.setResult(renderIcon(surface.resultIcon()));
        } finally {
            lock.unlock();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        session(event.getInventory()).ifPresent(session -> {
            if (event.getPlayer() instanceof Player player
                    && player.getUniqueId().equals(session.viewer().playerId())
                    && player.getOpenInventory().getTopInventory() != session.inventory()) {
                remove(session);
            }
        });

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        anvilSession(player.getUniqueId()).ifPresent(session -> {
            if (event.getView() != session.anvilView()) {
                return;
            }
            if (!session.completed()) {
                cancelTextInput(player, session, List.of(renameText(session)));
            }
            remove(session);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPluginEnable(PluginEnableEvent event) {
        String pluginName = event.getPlugin().getName();
        if ("Nexo".equalsIgnoreCase(pluginName)) {
            registerRefreshEvent("com.nexomc.nexo.api.events.NexoItemsLoadedEvent", CustomItemRegistry.NEXO);
            refreshProviderSessions(CustomItemRegistry.NEXO);
        } else if ("Oraxen".equalsIgnoreCase(pluginName)) {
            refreshProviderSessions(CustomItemRegistry.ORAXEN);
        } else if ("ItemsAdder".equalsIgnoreCase(pluginName)) {
            registerRefreshEvent("dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent", CustomItemRegistry.ITEMSADDER);
            refreshProviderSessions(CustomItemRegistry.ITEMSADDER);
        } else if ("CraftEngine".equalsIgnoreCase(pluginName)) {
            registerRefreshEvent("net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent", CustomItemRegistry.CRAFTENGINE);
            refreshProviderSessions(CustomItemRegistry.CRAFTENGINE);
        }
    }

    private void handleInventoryClick(InventoryClickEvent event, PaperInventoryUiSession session) {
        event.setCancelled(true);
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= session.screen().size()) {
            return;
        }

        Optional<UiButton> button = session.page().buttonAt(rawSlot);
        if (button.isEmpty() || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        UiClick click = new UiClick(
                session,
                session.viewer(),
                session.screen(),
                session.page(),
                button.get(),
                rawSlot,
                clickType(event.getClick())
        );
        try {
            button.get().handler().handle(click);
        } catch (Exception exception) {
            plugin.getLogger().log(
                    Level.WARNING,
                    "UI click failed screen=" + session.screen().id()
                            + " page=" + session.page().id()
                            + " slot=" + rawSlot
                            + " player=" + player.getName(),
                    exception
            );
        }
    }

    private synchronized void openNow(Player player, PaperUiSession session) {
        PaperUiSession previous = sessionsByViewer.get(player.getUniqueId());
        if (previous != null) {
            closeNow(player, previous);
        }
        sessionsByViewer.put(player.getUniqueId(), session);
        sessionsById.put(session.id(), session);

        boolean opened = true;
        if (session instanceof PaperInventoryUiSession inventorySession) {
            inventoryRenderer.open(player, inventorySession);
        } else if (session instanceof PaperBookUiSession bookSession) {
            bookRenderer.open(player, bookSession);
        } else if (session instanceof PaperTextInputUiSession inputSession
                && inputSession.surface() instanceof AnvilTextInputSurface) {
            opened = anvilRenderer.open(player, inputSession);
        } else if (session instanceof PaperTextInputUiSession inputSession
                && inputSession.surface() instanceof VirtualSignTextInputSurface) {
            opened = virtualSignRenderer.open(player, inputSession);
        } else {
            opened = false;
        }

        if (!opened) {
            plugin.getLogger().warning("Failed to open UI surface " + session.surface().id()
                    + " type=" + session.surface().type()
                    + " player=" + player.getName());
            remove(session);
        }
    }

    private synchronized void refreshNow(Player player, PaperUiSession session) {
        if (session.closed()) {
            return;
        }
        if (session instanceof PaperInventoryUiSession inventorySession) {
            inventoryRenderer.render(player, inventorySession);
        } else if (session instanceof PaperBookUiSession bookSession) {
            bookRenderer.open(player, bookSession);
        } else if (session instanceof PaperTextInputUiSession inputSession
                && inputSession.surface() instanceof AnvilTextInputSurface) {
            anvilRenderer.refresh(inputSession);
        } else if (session instanceof PaperTextInputUiSession inputSession
                && inputSession.surface() instanceof VirtualSignTextInputSurface) {
            virtualSignRenderer.open(player, inputSession);
        }
    }

    private synchronized void closeNow(Player player, PaperUiSession session) {
        remove(session);
        if (session instanceof PaperInventoryUiSession inventorySession
                && player.getOpenInventory().getTopInventory() == inventorySession.inventory()) {
            player.closeInventory();
        } else if (session instanceof PaperTextInputUiSession inputSession
                && inputSession.surface() instanceof AnvilTextInputSurface
                && inputSession.anvilView() != null
                && player.getOpenInventory() == inputSession.anvilView()) {
            player.closeInventory();
        } else if (session instanceof PaperTextInputUiSession inputSession
                && inputSession.surface() instanceof VirtualSignTextInputSurface) {
            virtualSignRenderer.restore(player, inputSession);
        }
    }

    private synchronized void remove(PaperUiSession session) {
        session.markClosed();
        if (sessionsByViewer.get(session.viewer().playerId()) == session) {
            sessionsByViewer.remove(session.viewer().playerId());
        }
        sessionsById.remove(session.id());
    }

    private Optional<PaperInventoryUiSession> session(Inventory inventory) {
        synchronized (this) {
            if (!(inventory.getHolder() instanceof PaperUiHolder holder)) {
                return Optional.empty();
            }
            PaperUiSession session = sessionsById.get(holder.sessionId());
            return session instanceof PaperInventoryUiSession inventorySession
                    ? Optional.of(inventorySession)
                    : Optional.empty();
        }
    }

    private Optional<PaperTextInputUiSession> anvilSession(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return Optional.empty();
        }
        return anvilSession(player.getUniqueId())
                .filter(session -> session.anvilView() != null && event.getView() == session.anvilView());
    }

    private synchronized Optional<PaperTextInputUiSession> anvilSession(UUID playerId) {
        PaperUiSession session = sessionsByViewer.get(playerId);
        return session instanceof PaperTextInputUiSession inputSession
                && inputSession.surface() instanceof AnvilTextInputSurface
                ? Optional.of(inputSession)
                : Optional.empty();
    }

    private synchronized Optional<PaperTextInputUiSession> virtualSignSession(UUID playerId) {
        PaperUiSession session = sessionsByViewer.get(playerId);
        return session instanceof PaperTextInputUiSession inputSession
                && inputSession.surface() instanceof VirtualSignTextInputSurface
                ? Optional.of(inputSession)
                : Optional.empty();
    }

    private void submitAnvilInput(Player player, PaperTextInputUiSession session) {
        submitTextInput(player, session, List.of(renameText(session)));
    }

    private void submitTextInput(Player player, PaperTextInputUiSession session, List<String> lines) {
        TextInputSurface surface = session.surface();
        Optional<Component> validationError = surface.validator().validate(lines);
        if (validationError.isPresent()) {
            player.sendMessage(validationError.get());
            return;
        }

        session.completed(true);
        UiTextInput input = new UiTextInput(session, session.viewer(), surface, lines);
        try {
            surface.submitHandler().handle(input);
        } catch (Exception exception) {
            plugin.getLogger().log(
                    Level.WARNING,
                    "UI text input submit failed surface=" + surface.id() + " player=" + player.getName(),
                    exception
            );
        } finally {
            closeNow(player, session);
        }
    }

    private void cancelTextInput(Player player, PaperTextInputUiSession session, List<String> lines) {
        TextInputSurface surface = session.surface();
        UiTextInput input = new UiTextInput(session, session.viewer(), surface, lines);
        try {
            surface.cancelHandler().handle(input);
        } catch (Exception exception) {
            plugin.getLogger().log(
                    Level.WARNING,
                    "UI text input cancel failed surface=" + surface.id() + " player=" + player.getName(),
                    exception
            );
        }
    }

    private String renameText(PaperTextInputUiSession session) {
        return session.anvilView() == null || session.anvilView().getRenameText() == null
                ? ""
                : session.anvilView().getRenameText();
    }

    private org.bukkit.inventory.ItemStack renderIcon(UiIcon icon) {
        return iconRenderer.item(icon);
    }

    private void runOnPlayer(UiOwner owner, Player player, Runnable command) {
        lock.lock();
        try {
            if (plugin.getServer().isOwnedByCurrentRegion(player)) {
                command.run();
                return;
            }
        } finally {
            lock.unlock();
        }
        schedule(owner, player, command);
    }

    private void schedule(UiOwner owner, Player player, Runnable command) {
        tasks.run(
                new TaskOwner(owner.runtimeId(), owner.moduleId()),
                TaskContext.entity(new EntityRef(player.getWorld().getName(), player.getUniqueId())),
                TaskSchedule.now(),
                command
        );
    }

    private Optional<Player> player(UiViewerRef viewer) {
        Player player = plugin.getServer().getPlayer(viewer.playerId());
        return player != null && player.isOnline() ? Optional.of(player) : Optional.empty();
    }

    private UiClickType clickType(ClickType clickType) {
        return switch (clickType) {
            case LEFT -> UiClickType.LEFT;
            case RIGHT -> UiClickType.RIGHT;
            case SHIFT_LEFT -> UiClickType.SHIFT_LEFT;
            case SHIFT_RIGHT -> UiClickType.SHIFT_RIGHT;
            case MIDDLE -> UiClickType.MIDDLE;
            case DROP, CONTROL_DROP -> UiClickType.DROP;
            case NUMBER_KEY -> UiClickType.NUMBER_KEY;
            case DOUBLE_CLICK -> UiClickType.DOUBLE_CLICK;
            default -> UiClickType.UNKNOWN;
        };
    }

    private void registerProviderRefreshEvents() {
        registerRefreshEvent("com.nexomc.nexo.api.events.NexoItemsLoadedEvent", CustomItemRegistry.NEXO);
        registerRefreshEvent("dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent", CustomItemRegistry.ITEMSADDER);
        registerRefreshEvent("net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent", CustomItemRegistry.CRAFTENGINE);
    }

    private void registerEventHandlersIfEnabled() {
        if (eventHandlersRegistered || !plugin.isEnabled()) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        eventHandlersRegistered = true;
        registerProviderRefreshEvents();
        registerVirtualSignChangeHandler();
    }

    private void registerRefreshEvent(String className, String providerId) {
        synchronized (registeredRefreshEvents) {
            if (registeredRefreshEvents.contains(className)) {
                return;
            }
            try {
                Class<?> rawEventClass = Class.forName(className);
                if (!Event.class.isAssignableFrom(rawEventClass)) {
                    return;
                }
                @SuppressWarnings("unchecked")
                Class<? extends Event> eventClass = (Class<? extends Event>) rawEventClass;
                EventExecutor executor = (listener, event) -> refreshProviderSessions(providerId);
                plugin.getServer().getPluginManager().registerEvent(
                        eventClass,
                        this,
                        EventPriority.MONITOR,
                        executor,
                        plugin,
                        true
                );
                registeredRefreshEvents.add(className);
            } catch (ClassNotFoundException ignored) {
                // Custom item providers are optional.
            }
        }
    }

    private void refreshProviderSessions(String providerId) {
        List<PaperInventoryUiSession> sessions;
        synchronized (this) {
            sessions = sessionsById.values().stream()
                    .filter(PaperInventoryUiSession.class::isInstance)
                    .map(PaperInventoryUiSession.class::cast)
                    .filter(session -> session.screen().usesCustomProvider(providerId))
                    .toList();
        }
        for (PaperInventoryUiSession session : sessions) {
            refresh(session);
        }
    }

    private void registerVirtualSignChangeHandler() {
        try {
            Class<?> rawEventClass = Class.forName("io.papermc.paper.event.packet.UncheckedSignChangeEvent");
            if (!Event.class.isAssignableFrom(rawEventClass)) {
                return;
            }
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) rawEventClass;
            EventExecutor executor = (listener, event) -> handleVirtualSignChange(event);
            plugin.getServer().getPluginManager().registerEvent(
                    eventClass,
                    this,
                    EventPriority.HIGHEST,
                    executor,
                    plugin,
                    true
            );
        } catch (ClassNotFoundException ignored) {
            // Virtual sign input is optional across Paper versions.
        }
    }

    private void handleVirtualSignChange(Event event) {
        if (!(event instanceof PlayerEvent playerEvent)) {
            return;
        }
        Player player = playerEvent.getPlayer();
        lock.lock();
        try {
            PaperTextInputUiSession session = virtualSignSession(player.getUniqueId()).orElse(null);
            if (session == null || session.closed()) {
                return;
            }
            Method setCancelled = event.getClass().getMethod("setCancelled", boolean.class);
            setCancelled.invoke(event, true);
            Method getEditedBlockPosition = event.getClass().getMethod("getEditedBlockPosition");
            Object editedPosition = getEditedBlockPosition.invoke(event);
            if (editedPosition instanceof BlockPosition blockPosition && !blockPosition.equals(session.signPosition())) {
                return;
            }
            Method linesMethod = event.getClass().getMethod("lines");
            Object rawLines = linesMethod.invoke(event);
            if (!(rawLines instanceof List<?> components)) {
                return;
            }
            List<String> lines = components.stream()
                    .filter(Component.class::isInstance)
                    .map(Component.class::cast)
                    .map(plainText::serialize)
                    .toList();
            submitTextInput(player, session, lines);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            plugin.getLogger().log(Level.WARNING, "Virtual sign input failed for player=" + player.getName(), exception);
        } finally {
            lock.unlock();
        }
    }
}
