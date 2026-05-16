package dev.beryl.lattice.paper.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.beryl.lattice.module.ModuleId;
import dev.beryl.lattice.task.TaskContextType;
import dev.beryl.lattice.task.TaskOwner;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;

class PaperTaskServiceTest {
    @Test
    void ownerCancellationClearsContextDiagnostics() throws Exception {
        PaperTaskService service = allocate(PaperTaskService.class);
        setField(service, "handlesByOwner", new LinkedHashMap<>());
        setField(service, "contextsByHandle", new IdentityHashMap<>());

        TaskOwner owner = new TaskOwner("test", ModuleId.of("tasks"));
        PaperTaskHandle handle = new PaperTaskHandle();
        Method track = PaperTaskService.class.getDeclaredMethod(
                "track",
                TaskOwner.class,
                PaperTaskHandle.class,
                TaskContextType.class
        );
        track.setAccessible(true);
        track.invoke(service, owner, handle, TaskContextType.ASYNC);

        assertEquals(1, service.diagnostics().activeTasks());

        service.cancel(owner);

        assertTrue(handle.cancelled());
        assertEquals(0, service.diagnostics().activeTasks());
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Method allocateInstance = unsafe.getClass().getMethod("allocateInstance", Class.class);
        return type.cast(allocateInstance.invoke(unsafe, type));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
