package dev.beryl.lattice.paper.command;

import dev.beryl.lattice.command.CommandContext;
import dev.beryl.lattice.command.CommandExceptionMapper;
import dev.beryl.lattice.command.CommandNode;
import dev.beryl.lattice.command.CommandParseException;
import dev.beryl.lattice.command.CommandParser;
import dev.beryl.lattice.command.CommandPermissionException;
import dev.beryl.lattice.command.CommandPermissions;
import dev.beryl.lattice.command.CommandSenderRef;
import dev.beryl.lattice.command.CommandService;
import dev.beryl.lattice.command.CommandUsage;
import dev.beryl.lattice.command.CommandUsageException;
import dev.beryl.lattice.command.ParsedCommand;
import dev.beryl.lattice.diagnostics.CommandDiagnostics;
import dev.beryl.lattice.util.Preconditions;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperCommandRegistrar implements CommandService {
    private final JavaPlugin plugin;
    private final CommandExceptionMapper exceptionMapper;
    private final CommandParser parser = new CommandParser();
    private final List<CommandNode> commands = new ArrayList<>();

    public PaperCommandRegistrar(JavaPlugin plugin) {
        this(plugin, PaperCommandRegistrar::defaultExceptionMessage);
    }

    public PaperCommandRegistrar(JavaPlugin plugin, CommandExceptionMapper exceptionMapper) {
        this.plugin = Preconditions.requireNonNull(plugin, "plugin");
        this.exceptionMapper = Preconditions.requireNonNull(exceptionMapper, "exceptionMapper");
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            synchronized (commands) {
                for (CommandNode command : commands) {
                    event.registrar().register(
                            plugin.getPluginMeta(),
                            command.name(),
                            command.description(),
                            command.aliases(),
                            new LatticeBasicCommand(command)
                    );
                }
            }
        });
    }

    @Override
    public void register(CommandNode command) {
        Preconditions.requireNonNull(command, "command");
        synchronized (commands) {
            commands.add(command);
        }
    }

    @Override
    public void unregisterAll() {
        synchronized (commands) {
            commands.clear();
        }
    }

    @Override
    public List<CommandDiagnostics> commands() {
        synchronized (commands) {
            return commands.stream()
                    .map(command -> new CommandDiagnostics(
                            command.name(),
                            command.aliases(),
                            command.description(),
                            command.permission().map(permission -> permission.value()).orElse(null),
                            CommandUsage.help(command)
                    ))
                    .toList();
        }
    }

    private final class LatticeBasicCommand implements BasicCommand {
        private final CommandNode root;

        private LatticeBasicCommand(CommandNode root) {
            this.root = root;
        }

        @Override
        public void execute(CommandSourceStack source, String[] args) {
            CommandSender sender = source.getSender();
            CommandSenderRef senderRef = sender(sender);
            ParsedCommand parsed = null;
            CommandContext context = new CommandContext(senderRef, Map.of(), sender::sendMessage);
            try {
                parsed = parser.parse(root, args);
                context = new CommandContext(senderRef, parsed.arguments(), sender::sendMessage);

                var denied = CommandPermissions.firstDenied(
                        parsed.path(),
                        permission -> sender.hasPermission(permission.value())
                );
                if (denied.isPresent()) {
                    handleFailure(
                            CommandFailureCategory.PERMISSION_DENIED,
                            new CommandPermissionException(parsed.commandPath(), denied.get()),
                            context,
                            parsed,
                            args
                    );
                    return;
                }

                if (parsed.node().executor().isEmpty()) {
                    handleFailure(
                            CommandFailureCategory.MISSING_EXECUTOR,
                            new CommandUsageException("No executable command matched.", parsed.usage()),
                            context,
                            parsed,
                            args
                    );
                    return;
                }

                parsed.node().executor().orElseThrow().execute(context);
            } catch (CommandParseException exception) {
                handleFailure(CommandFailureCategory.PARSE_ERROR, exception, context, parsed, args);
            } catch (Exception exception) {
                handleFailure(CommandFailureCategory.EXECUTOR_EXCEPTION, exception, context, parsed, args);
            }
        }

        @Override
        public Collection<String> suggest(CommandSourceStack source, String[] args) {
            return parser.suggest(
                    root,
                    args,
                    path -> CommandPermissions.canUsePath(
                            path,
                            permission -> source.getSender().hasPermission(permission.value())
                    )
            );
        }

        @Override
        public boolean canUse(CommandSender sender) {
            return root.permission().map(permission -> sender.hasPermission(permission.value())).orElse(true);
        }

        @Override
        public String permission() {
            return root.permission().map(permission -> permission.value()).orElse(null);
        }

        private void handleFailure(
                CommandFailureCategory category,
                Throwable throwable,
                CommandContext context,
                ParsedCommand parsed,
                String[] args
        ) {
            context.reply(exceptionMapper.map(throwable, context));
            logFailure(category, throwable, context.sender(), parsed, args);
        }

        private void logFailure(
                CommandFailureCategory category,
                Throwable throwable,
                CommandSenderRef sender,
                ParsedCommand parsed,
                String[] args
        ) {
            if (!plugin.getLogger().isLoggable(category.level())) {
                return;
            }

            String message = "Command failure"
                    + " category=" + category.label()
                    + " root=/" + root.name()
                    + " path=/" + (parsed == null ? root.name() : parsed.commandPath())
                    + " sender=" + senderDetails(sender)
                    + " rawArgs=" + Arrays.toString(args)
                    + " parsedArgs=" + (parsed == null ? "{}" : parsed.arguments());
            if (category.includeStackTrace()) {
                plugin.getLogger().log(category.level(), message, throwable);
            } else {
                plugin.getLogger().log(category.level(), message + " reason=" + throwable.getMessage());
            }
        }

        private String senderDetails(CommandSenderRef sender) {
            return sender.type()
                    + ":" + sender.name()
                    + sender.uniqueIdOptional().map(uuid -> ":" + uuid).orElse("");
        }

        private CommandSenderRef sender(CommandSender sender) {
            if (sender instanceof Player player) {
                return new CommandSenderRef(CommandSenderRef.Type.PLAYER, player.getName(), player.getUniqueId());
            }
            if (sender instanceof ConsoleCommandSender) {
                return new CommandSenderRef(CommandSenderRef.Type.CONSOLE, sender.getName(), null);
            }
            if (sender instanceof BlockCommandSender) {
                return new CommandSenderRef(CommandSenderRef.Type.COMMAND_BLOCK, sender.getName(), null);
            }
            UUID uniqueId = sourceUuid(sender);
            return new CommandSenderRef(CommandSenderRef.Type.OTHER, sender.getName(), uniqueId);
        }

        private UUID sourceUuid(CommandSender sender) {
            return sender instanceof Player player ? player.getUniqueId() : null;
        }
    }

    private static Component defaultExceptionMessage(Throwable throwable, CommandContext context) {
        if (throwable instanceof CommandPermissionException) {
            return Component.text("You do not have permission to use this command.", NamedTextColor.RED);
        }
        if (throwable instanceof CommandUsageException exception) {
            return Component.text(exception.getMessage(), NamedTextColor.RED)
                    .append(Component.newline())
                    .append(Component.text("Usage: " + exception.usage(), NamedTextColor.RED));
        }
        if (throwable instanceof CommandParseException exception) {
            Component message = Component.text(exception.getMessage(), NamedTextColor.RED);
            return exception.usage()
                    .map(usage -> message.append(Component.newline())
                            .append(Component.text("Usage: " + usage, NamedTextColor.RED)))
                    .orElse(message);
        }
        return Component.text("Command failed. Check the server console for details.", NamedTextColor.RED);
    }

    private enum CommandFailureCategory {
        PERMISSION_DENIED("permission_denied", Level.FINE, false),
        PARSE_ERROR("parse_error", Level.FINE, false),
        MISSING_EXECUTOR("missing_executor", Level.WARNING, false),
        EXECUTOR_EXCEPTION("executor_exception", Level.SEVERE, true);

        private final String label;
        private final Level level;
        private final boolean includeStackTrace;

        CommandFailureCategory(String label, Level level, boolean includeStackTrace) {
            this.label = label;
            this.level = level;
            this.includeStackTrace = includeStackTrace;
        }

        String label() {
            return label;
        }

        Level level() {
            return level;
        }

        boolean includeStackTrace() {
            return includeStackTrace;
        }
    }
}
