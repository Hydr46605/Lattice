package dev.beryl.lattice.paper.task;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.beryl.lattice.task.EntityRef;
import dev.beryl.lattice.task.TaskContext;
import dev.beryl.lattice.task.TaskOwner;
import dev.beryl.lattice.task.TaskSchedule;
import dev.beryl.lattice.module.ModuleId;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class PaperTaskServiceThreadSafetyTest {

    @Test
    void scheduleEntityThrowsOnAsyncThread() throws Exception {
        PaperTaskService service = allocate(PaperTaskService.class);
        setField(service, "lock", new ReentrantLock());
        setField(service, "handlesByOwner", new LinkedHashMap<>());
        setField(service, "contextsByHandle", new IdentityHashMap<>());

        TaskContext context = TaskContext.entity(new EntityRef("test_world", UUID.randomUUID()));
        TaskSchedule schedule = TaskSchedule.now();
        Consumer<Object> command = ignored -> {};
        Runnable retired = () -> {};

        // Use AtomicReference to capture exception from async thread
        AtomicReference<Throwable> capturedError = new AtomicReference<>();

        Thread asyncThread = new Thread(() -> {
            try {
                Method scheduleEntityMethod = PaperTaskService.class.getDeclaredMethod(
                        "scheduleEntity",
                        TaskContext.class,
                        TaskSchedule.class,
                        Consumer.class,
                        Runnable.class
                );
                scheduleEntityMethod.setAccessible(true);
                scheduleEntityMethod.invoke(service, context, schedule, command, retired);
            } catch (Throwable t) {
                capturedError.set(t);
            }
        }, "AsyncSomethingThread");
        asyncThread.start();
        asyncThread.join(1000);

        // THEN it rejects with clear error message
        assertNotNull(capturedError.get(), "Expected exception from async thread");
        Throwable cause = capturedError.get();
        if (cause instanceof java.lang.reflect.InvocationTargetException) {
            cause = cause.getCause();
        }
        assertTrue(cause instanceof IllegalStateException,
                "Expected IllegalStateException but got: " + cause.getClass().getName());
        assertTrue(cause.getMessage().contains("async"),
                "Exception message should contain 'async': " + cause.getMessage());
        assertTrue(cause.getMessage().contains("TaskContext.global()"),
                "Exception message should suggest TaskContext.global(): " + cause.getMessage());
    }

    @Test
    void scheduleEntityFromMainThreadDoesNotThrowForThreadValidation() throws Exception {
        PaperTaskService service = allocate(PaperTaskService.class);
        setField(service, "lock", new ReentrantLock());
        setField(service, "handlesByOwner", new LinkedHashMap<>());
        setField(service, "contextsByHandle", new IdentityHashMap<>());

        TaskContext context = TaskContext.entity(new EntityRef("test_world", UUID.randomUUID()));
        TaskSchedule schedule = TaskSchedule.now();
        Consumer<Object> command = ignored -> {};
        Runnable retired = () -> {};

        // Call from main thread should not throw IllegalStateException for thread validation
        // It may throw for entity not found, but that's expected and different from thread validation
        Method scheduleEntityMethod = PaperTaskService.class.getDeclaredMethod(
                "scheduleEntity",
                TaskContext.class,
                TaskSchedule.class,
                Consumer.class,
                Runnable.class
        );
        scheduleEntityMethod.setAccessible(true);

        Exception exception = assertThrows(Exception.class, () -> {
            scheduleEntityMethod.invoke(service, context, schedule, command, retired);
        });

        // Should not be a thread-safety error
        assertNotThreadValidationError(exception);
    }

    private void assertNotThreadValidationError(Exception exception) {
        String message = exception.getMessage() != null ? exception.getMessage() : "";
        // If it's an InvocationTargetException, check the cause
        if (exception.getCause() != null) {
            message = exception.getCause().getMessage() != null ? exception.getCause().getMessage() : "";
        }
        assertTrue(
                !message.contains("async thread") && !message.contains("must be called from"),
                "Exception should not be a thread validation error: " + message
        );
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        java.lang.reflect.Method allocateInstance = unsafe.getClass().getMethod("allocateInstance", Class.class);
        return type.cast(allocateInstance.invoke(unsafe, type));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
