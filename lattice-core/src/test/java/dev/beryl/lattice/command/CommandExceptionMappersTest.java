package dev.beryl.lattice.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class CommandExceptionMappersTest {
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
    private final CommandContext context = new CommandContext(
            new CommandSenderRef(CommandSenderRef.Type.CONSOLE, "Console", null),
            java.util.Map.of()
    );

    @Test
    void defaultMapperHandlesPermissionDenials() {
        String message = plain.serialize(CommandExceptionMappers.defaultMapper().map(
                new CommandPermissionException("sample reload", new PermissionNode("sample.reload")),
                context
        ));

        assertEquals("You do not have permission to use this command.", message);
    }

    @Test
    void defaultMapperIncludesUsageForUsageFailures() {
        String message = plain.serialize(CommandExceptionMappers.defaultMapper().map(
                new CommandUsageException("No executable command matched.", "/sample reload"),
                context
        ));

        assertEquals("No executable command matched.\nUsage: /sample reload", message);
    }

    @Test
    void defaultMapperIncludesUsageForParseFailures() {
        String message = plain.serialize(CommandExceptionMappers.defaultMapper().map(
                new CommandParseException("Invalid value for argument amount", "/sample give <amount>"),
                context
        ));

        assertEquals("Invalid value for argument amount\nUsage: /sample give <amount>", message);
    }

    @Test
    void defaultMapperUsesSafeFallbackForUnexpectedFailures() {
        String message = plain.serialize(CommandExceptionMappers.defaultMapper().map(
                new IllegalStateException("database password leaked here"),
                context
        ));

        assertEquals("Command failed. Check the server console for details.", message);
    }
}
