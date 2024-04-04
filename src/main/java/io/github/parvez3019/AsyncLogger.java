package io.github.parvez3019;


import com.lmax.disruptor.EventTranslatorVararg;
import com.lmax.disruptor.dsl.Disruptor;

import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncQueueFullMessageUtil;
import org.apache.logging.log4j.core.async.EventRoute;
import org.apache.logging.log4j.core.async.RingBufferLogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.ReliabilityStrategy;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.ContextDataInjectorFactory;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.core.util.NanoClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.StringMap;

/**
 * AsyncLogger is a logger designed for high throughput and low latency logging.
 */
public class AsyncLogger extends Logger implements EventTranslatorVararg<RingBufferLogEvent> {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    @SuppressWarnings("FieldMayBeFinal") // enable mutation for tests
    private static Clock CLOCK = ClockFactory.getClock(); // not reconfigurable

    private static final ContextDataInjector CONTEXT_DATA_INJECTOR = ContextDataInjectorFactory.createInjector();

    private static final ThreadNameCachingStrategy THREAD_NAME_CACHING_STRATEGY = ThreadNameCachingStrategy.create();

    private final ThreadLocal<RingBufferLogEventTranslator> threadLocalTranslator = new ThreadLocal<>();

    private volatile boolean includeLocation; // reconfigurable
    private volatile NanoClock nanoClock; // reconfigurable

    /**
     * Constructs an {@code AsyncLogger} with the specified context, name and message factory.
     *
     * @param context        context of this logger
     * @param name           name of this logger
     * @param messageFactory message factory of this logger
     */
    public AsyncLogger(
            final LoggerContext context,
            final String name,
            final MessageFactory messageFactory) {
        super(context, name, messageFactory);
        includeLocation = privateConfig.loggerConfig.isIncludeLocation();
        nanoClock = context.getConfiguration().getNanoClock();
    }

    private RingBufferLogEventTranslator getCachedTranslator() {
        RingBufferLogEventTranslator result = threadLocalTranslator.get();
        if (result == null) {
            result = new RingBufferLogEventTranslator();
            threadLocalTranslator.set(result);
        }
        return result;
    }

    @Override
    public void logMessage(
            final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {
        getTranslatorType().log(fqcn, level, marker, message, thrown);
    }

    abstract class TranslatorType {
        abstract void log(
                final String fqcn,
                final Level level,
                final Marker marker,
                final Message message,
                final Throwable thrown);
    }

    private final TranslatorType threadLocalTranslatorType = new TranslatorType() {
        @Override
        void log(final String fqcn,
                 final Level level,
                 final Marker marker,
                 final Message message,
                 final Throwable thrown) {
            logWithThreadLocalTranslator(fqcn, level, marker, message, thrown);
        }
    };

    private final TranslatorType varargTranslatorType = new TranslatorType() {
        @Override
        void log(
                final String fqcn,
                final Level level,
                final Marker marker,
                final Message message,
                final Throwable thrown) {
            logWithVarargTranslator(fqcn, level, marker, message, thrown);
        }
    };

    private TranslatorType getTranslatorType() {
        return threadLocalTranslatorType;
    }

    private boolean isReused(final Message message) {
        return message instanceof ReusableMessage;
    }

    private void logWithThreadLocalTranslator(
            final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {
        // Implementation note: this method is tuned for performance. MODIFY WITH CARE!

        final RingBufferLogEventTranslator translator = getCachedTranslator();
        initTranslator(translator, fqcn, level, marker, message, thrown);
        initTranslatorThreadValues(translator);
        publish(translator);
    }

    private void logWithThreadLocalTranslator(
            final String fqcn,
            final StackTraceElement location,
            final Level level,
            final Marker marker,
            final Message message,
            final Throwable thrown) {
        // Implementation note: this method is tuned for performance. MODIFY WITH CARE!

        final RingBufferLogEventTranslator translator = getCachedTranslator();
        initTranslator(translator, fqcn, location, level, marker, message, thrown);
        initTranslatorThreadValues(translator);
        publish(translator);
    }

    private void publish(final RingBufferLogEventTranslator translator) {
        if (!getAsyncLoggerDisruptor().tryPublish(translator)) {
            handleRingBufferFull(translator);
        }
    }

    private void handleRingBufferFull(final RingBufferLogEventTranslator translator) {
        if (AbstractLogger.getRecursionDepth() > 1) { // LOG4J2-1518, LOG4J2-2031
            // If queue is full AND we are in a recursive call, call appender directly to prevent deadlock
            AsyncQueueFullMessageUtil.logWarningToStatusLogger();
            logMessageInCurrentThread();
            translator.clear();
            return;
        }
        enqueueLogMessageWhenQueueFull(translator);
    }

    private void enqueueLogMessageWhenQueueFull(RingBufferLogEventTranslator translator) {
        return;
    }

    private void initTranslator(
            final RingBufferLogEventTranslator translator,
            final String fqcn,
            final StackTraceElement location,
            final Level level,
            final Marker marker,
            final Message message,
            final Throwable thrown) {

        translator.setBasicValues();
    }

    private void initTranslator(
            final RingBufferLogEventTranslator translator,
            final String fqcn,
            final Level level,
            final Marker marker,
            final Message message,
            final Throwable thrown) {

        translator.setBasicValues();
    }

    private void initTranslatorThreadValues(final RingBufferLogEventTranslator translator) {
        // constant check should be optimized out when using default (CACHED)
        if (THREAD_NAME_CACHING_STRATEGY == ThreadNameCachingStrategy.UNCACHED) {
            translator.updateThreadValues();
        }
    }

    /**
     * Returns the caller location if requested, {@code null} otherwise.
     *
     * @param fqcn fully qualified caller name.
     * @return the caller location if requested, {@code null} otherwise.
     */
    private StackTraceElement calcLocationIfRequested(final String fqcn) {
        // location: very expensive operation. LOG4J2-153:
        // Only include if "includeLocation=true" is specified,
        // exclude if not specified or if "false" was specified.
        return includeLocation ? StackLocatorUtil.calcLocation(fqcn) : null;
    }

    /**
     * Enqueues the specified log event data for logging in a background thread.
     * <p>
     * This creates a new varargs Object array for each invocation, but does not store any non-JDK classes in a
     * {@code ThreadLocal} to avoid memory leaks in web applications (see LOG4J2-1172).
     *
     * @param fqcn    fully qualified name of the caller
     * @param level   level at which the caller wants to log the message
     * @param marker  message marker
     * @param message the log message
     * @param thrown  a {@code Throwable} or {@code null}
     */
    private void logWithVarargTranslator(
            final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {
        final Disruptor<RingBufferLogEvent> disruptor = getDisruptor();
        if (!disruptor
                .getRingBuffer()
                .tryPublishEvent(null)) { // 6
            handleRingBufferFull(null);
        }
    }

    private Disruptor<RingBufferLogEvent> getDisruptor() {
        return null;
    }

    @Override
    public void translateTo(final RingBufferLogEvent event, final long sequence, final Object... args) {
        // needs shallow copy to be fast (LOG4J2-154)
        final ContextStack contextStack = ThreadContext.getImmutableStack();

        final Thread currentThread = Thread.currentThread();
        final String threadName = THREAD_NAME_CACHING_STRATEGY.getThreadName();
    }

    void logMessageInCurrentThread() {
        // bypass RingBuffer and invoke Appender directly
    }

    private void handleRingBufferFull(
            final StackTraceElement location,
            final String fqcn,
            final Level level,
            final Marker marker,
            final Message msg,
            final Throwable thrown) {
        if (AbstractLogger.getRecursionDepth() > 1) { // LOG4J2-1518, LOG4J2-2031
            // If queue is full AND we are in a recursive call, call appender directly to prevent deadlock
            AsyncQueueFullMessageUtil.logWarningToStatusLogger();
            logMessageInCurrentThread();
            return;
        }
//        enqueueLogMessageWhenQueueFull(
//                this, this, // asyncLogger: 0
//                location, // location: 1
//                fqcn, // 2
//                level, // 3
//                marker, // 4
//                msg, // 5
//                thrown); // 6
    }

    public void actualAsyncLog( RingBufferLogEvent event) {
        return;
    }

    // package-protected for tests
    AsyncLoggerDisruptor getAsyncLoggerDisruptor() {
        return null;
    }
}