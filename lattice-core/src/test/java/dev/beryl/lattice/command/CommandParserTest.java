package dev.beryl.lattice.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CommandParserTest {
    private final CommandParser parser = new CommandParser();

    @Test
    void parsesChildCommandArguments() throws Exception {
        CommandNode root = CommandNode.command("lattice")
                .child(CommandNode.command("reload")
                        .argument(new CommandArgument<>("force", boolean.class, false))
                        .build())
                .build();

        ParsedCommand parsed = parser.parse(root, new String[]{"reload", "yes"});

        assertEquals("reload", parsed.node().name());
        assertEquals(true, parsed.arguments().get("force"));
    }

    @Test
    void parsesNestedSubcommandPathAndUsage() throws Exception {
        CommandNode root = CommandNode.command("lattice")
                .child(CommandNode.command("module")
                        .child(CommandNode.command("enable")
                                .argument(new CommandArgument<>("name", String.class, true))
                                .build())
                        .build())
                .build();

        ParsedCommand parsed = parser.parse(root, new String[]{"module", "enable", "economy"});

        assertEquals(List.of("lattice", "module", "enable"), parsed.path().stream().map(CommandNode::name).toList());
        assertEquals("lattice module enable", parsed.commandPath());
        assertEquals("/lattice module enable <name>", parsed.usage());
        assertEquals("economy", parsed.arguments().get("name"));
    }

    @Test
    void rejectsMissingRequiredArguments() {
        CommandNode root = CommandNode.command("lattice")
                .argument(new CommandArgument<>("amount", int.class, true))
                .build();

        assertThrows(CommandParseException.class, () -> parser.parse(root, new String[0]));
    }

    @Test
    void parseErrorsExposeUsage() {
        CommandNode root = CommandNode.command("tabular")
                .child(CommandNode.command("reload")
                        .argument(new CommandArgument<>("force", boolean.class, false))
                        .build())
                .build();

        CommandParseException exception = assertThrows(
                CommandParseException.class,
                () -> parser.parse(root, new String[]{"reload", "maybe"})
        );

        assertEquals("Invalid boolean value for argument force", exception.getMessage());
        assertEquals("/tabular reload [force]", exception.usage().orElseThrow());
    }

    @Test
    void suggestsChildNamesAndAliases() {
        CommandNode root = CommandNode.command("lattice")
                .child(CommandNode.command("reload").alias("rl").build())
                .child(CommandNode.command("status").build())
                .build();

        assertEquals(List.of("reload", "rl"), parser.suggest(root, new String[]{"r"}));
    }

    @Test
    void filtersSuggestionsWithPermissionPredicate() {
        CommandNode root = CommandNode.command("tabular")
                .child(CommandNode.command("apply")
                        .permission("tabular.command")
                        .build())
                .child(CommandNode.command("reload")
                        .permission("tabular.command.reload")
                        .build())
                .build();
        Set<String> granted = Set.of("tabular.command");

        List<String> suggestions = parser.suggest(
                root,
                new String[]{""},
                path -> CommandPermissions.canUsePath(
                        path,
                        permission -> granted.contains(permission.value())
                )
        );

        assertEquals(List.of("apply"), suggestions);
    }

    @Test
    void permissionChecksRequireEveryNodeInPath() throws Exception {
        CommandNode root = CommandNode.command("tabular")
                .permission("tabular.command")
                .child(CommandNode.command("reload")
                        .permission("tabular.command.reload")
                        .build())
                .build();
        ParsedCommand parsed = parser.parse(root, new String[]{"reload"});

        assertFalse(CommandPermissions.canUse(parsed, permission -> Set.of("tabular.command").contains(permission.value())));
        assertTrue(CommandPermissions.canUse(
                parsed,
                permission -> Set.of("tabular.command", "tabular.command.reload").contains(permission.value())
        ));
    }

    @Test
    void rejectsDuplicateChildNamesAndAliases() {
        assertThrows(IllegalArgumentException.class, () -> CommandNode.command("tabular")
                .child(CommandNode.command("reload").alias("rl").build())
                .child(CommandNode.command("refresh").alias("RL").build())
                .build());
    }

    @Test
    void rejectsDuplicateAliasOnSameCommand() {
        assertThrows(IllegalArgumentException.class, () -> CommandNode.command("reload")
                .alias("rl")
                .alias("RL")
                .build());
    }

    @Test
    void rejectsRequiredArgumentAfterOptionalArgument() {
        assertThrows(IllegalArgumentException.class, () -> CommandNode.command("lattice")
                .argument(new CommandArgument<>("force", boolean.class, false))
                .argument(new CommandArgument<>("module", String.class, true))
                .build());
    }
}
