package dev.beryl.lattice.paper.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.beryl.lattice.command.CommandExceptionMapper;
import dev.beryl.lattice.command.CommandExceptionMappers;
import dev.beryl.lattice.command.CommandNode;
import dev.beryl.lattice.command.CommandService;
import dev.beryl.lattice.lifecycle.LatticeBuilder;
import dev.beryl.lattice.lifecycle.LatticeRuntime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class LatticePaperDefaultServicesTest {
    @Test
    void defaultCommandServiceSupplierIsNotCalledWhenCommandServiceExists() {
        LatticeBuilder builder = LatticeRuntime.builder("paper-defaults");
        RecordingCommandService existingCommands = new RecordingCommandService();
        AtomicInteger supplierCalls = new AtomicInteger();

        builder.service(LatticeRuntime.COMMAND_SERVICE, existingCommands);

        LatticePaper.defaultCommandService(builder, mapper -> {
            supplierCalls.incrementAndGet();
            return new RecordingCommandService();
        });

        assertEquals(0, supplierCalls.get());
        assertSame(existingCommands, builder.requireService(LatticeRuntime.COMMAND_SERVICE));
    }

    @Test
    void defaultCommandServiceSupplierIsRegisteredWhenCommandServiceIsMissing() {
        LatticeBuilder builder = LatticeRuntime.builder("paper-defaults");
        RecordingCommandService defaultCommands = new RecordingCommandService();
        AtomicInteger supplierCalls = new AtomicInteger();

        builder.defaultService(LatticeRuntime.COMMAND_EXCEPTION_MAPPER, CommandExceptionMappers.defaultMapper());

        LatticePaper.defaultCommandService(builder, mapper -> {
            supplierCalls.incrementAndGet();
            return defaultCommands;
        });

        assertEquals(1, supplierCalls.get());
        assertSame(defaultCommands, builder.requireService(LatticeRuntime.COMMAND_SERVICE));
    }

    @Test
    void defaultCommandServiceFactoryReceivesConfiguredCommandExceptionMapper() {
        LatticeBuilder builder = LatticeRuntime.builder("paper-defaults");
        RecordingCommandService defaultCommands = new RecordingCommandService();
        CommandExceptionMapper mapper = (throwable, context) -> Component.text("custom failure");
        AtomicInteger supplierCalls = new AtomicInteger();
        AtomicReference<CommandExceptionMapper> receivedMapper = new AtomicReference<>();

        builder.commandExceptionMapper(mapper);

        LatticePaper.defaultCommandService(builder, commandExceptionMapper -> {
            supplierCalls.incrementAndGet();
            receivedMapper.set(commandExceptionMapper);
            return defaultCommands;
        });

        assertEquals(1, supplierCalls.get());
        assertSame(mapper, receivedMapper.get());
        assertSame(defaultCommands, builder.requireService(LatticeRuntime.COMMAND_SERVICE));
    }

    private static final class RecordingCommandService implements CommandService {
        @Override
        public void register(CommandNode command) {
        }

        @Override
        public void unregisterAll() {
        }
    }
}
