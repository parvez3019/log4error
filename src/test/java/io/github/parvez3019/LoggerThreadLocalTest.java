package io.github.parvez3019;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class LoggerThreadLocalTest {

    private final LoggerThreadLocal threadLocal = new LoggerThreadLocal();

    @AfterEach
    void cleanup() {
        threadLocal.remove();
    }

    @Test
    void getLoggerCreatesWhenAbsent() {
        assertNull(threadLocal.get());
        Logger created = threadLocal.getLogger();
        assertNotNull(created);
        assertSame(created, threadLocal.get());
    }

    @Test
    void removeDoesNotInventLoggerWhenUnset() {
        assertNull(threadLocal.get());
        threadLocal.remove();
        assertNull(threadLocal.get());
    }

    @Test
    void removeClearsBufferAndUnbinds() {
        Logger logger = new Logger();
        logger.info("x");
        threadLocal.set(logger);
        threadLocal.remove();
        assertNull(threadLocal.get());
        assertEquals(0, logger.bufferedSize());
    }

    @Test
    void isolationAcrossThreads() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Logger> other = new AtomicReference<>();

        threadLocal.set(new Logger());
        Logger main = threadLocal.get();

        Thread t = new Thread(() -> {
            other.set(threadLocal.getLogger());
            ready.countDown();
            try {
                done.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                threadLocal.remove();
            }
        });
        t.start();
        ready.await();
        assertNotSame(main, other.get());
        done.countDown();
        t.join();
    }
}
