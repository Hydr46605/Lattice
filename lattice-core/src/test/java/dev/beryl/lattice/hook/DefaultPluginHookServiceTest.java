package dev.beryl.lattice.hook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultPluginHookServiceTest {
    private static final HookKey<ExampleHook> KEY = new HookKey<>("tabular.nametag", ExampleHook.class);

    @Test
    void publishesAndFindsContributionsByPriority() {
        DefaultPluginHookService hooks = new DefaultPluginHookService("tabular");
        ExampleHook low = new ExampleHook("low");
        ExampleHook high = new ExampleHook("high");

        hooks.publish(KEY, low, HookPriority.LOW);
        hooks.publish(KEY, high, HookPriority.HIGH);

        List<HookContribution<ExampleHook>> contributions = hooks.contributions(KEY);
        assertEquals(List.of(high, low), contributions.stream().map(HookContribution::hook).toList());
        assertSame(high, hooks.first(KEY).orElseThrow());
        assertEquals("tabular.nametag", hooks.hooks().get(0).key());
    }

    @Test
    void registrationCloseUnpublishesContribution() {
        DefaultPluginHookService hooks = new DefaultPluginHookService("penalties");
        HookRegistration<ExampleHook> registration = hooks.publish(KEY, new ExampleHook("pk"));

        registration.close();

        assertTrue(hooks.contributions(KEY).isEmpty());
        assertTrue(hooks.hooks().isEmpty());
    }

    private record ExampleHook(String id) {
    }
}
