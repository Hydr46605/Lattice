package dev.beryl.lattice.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class CommandExceptionMappers {
    private CommandExceptionMappers() {
    }

    public static CommandExceptionMapper defaultMapper() {
        return CommandExceptionMappers::defaultMessage;
    }

    public static Component defaultMessage(Throwable throwable, CommandContext context) {
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
}
