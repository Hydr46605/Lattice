package dev.beryl.lattice.paper.hook;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.beryl.lattice.hook.HookKey;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaperPluginHookServiceTest {
    @Test
    void rejectsDifferentHookKeysForSameServiceChannel() {
        Map<Class<?>, HookKey<?>> published = new LinkedHashMap<>();
        HookKey<ExampleHook> first = new HookKey<>("example.first", ExampleHook.class);
        HookKey<ExampleHook> second = new HookKey<>("example.second", ExampleHook.class);
        published.put(first.type(), first);

        assertDoesNotThrow(() -> PaperPluginHookService.validateServiceChannel(published, first));
        assertThrows(
                IllegalArgumentException.class,
                () -> PaperPluginHookService.validateServiceChannel(published, second)
        );
    }

    private interface ExampleHook {
    }
}
