package io.github.parvez3019;


import com.lmax.disruptor.EventTranslatorVararg;
import com.lmax.disruptor.dsl.Disruptor;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.async.;;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.RingBufferLogEvent;
import org.apache.logging.log4j.core.async.RingBufferLogEventTranslator;
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
     * @param context context of this logger
     * @param name name of this logger
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

    /**
     * Enqueues the specified log event data for logging in a background thread.
     * <p>
     * This re-uses a {@code RingBufferLogEventTranslator} instance cached in a {@code ThreadLocal} to avoid creating
     * unnecessary objects with each event.
     *
     * @param fqcn fully qualified name of the caller
     * @param level level at which the caller wants to log the message
     * @param marker message marker
     * @param message the log message
     * @param thrown a {@code Throwable} or {@code null}
     */
    private void logWithThreadLocalTranslator(
            final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {
        // Implementation note: this method is tuned for performance. MODIFY WITH CARE!

        final RingBufferLogEventTranslator translator = getCachedTranslator();
        initTranslator(translator, fqcn, level, marker, message, thrown);
        initTranslatorThreadValues(translator);
        publish(translator);
    }

    /**
     * Enqueues the specified log event data for logging in a background thread.
     * <p>
     * This re-uses a {@code RingBufferLogEventTranslator} instance cached in a {@code ThreadLocal} to avoid creating
     * unnecessary objects with each event.
     *
     * @param fqcn fully qualified name of the caller
     * @param location the Location of the caller.
     * @param level level at which the caller wants to log the message
     * @param marker message marker
     * @param message the log message
     * @param thrown a {@code Throwable} or {@code null}
     */
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
        if (!loggerDisruptor.tryPublish(translator)) {
            handleRingBufferFull(translator);
        }
    }

    private void handleRingBufferFull(final RingBufferLogEventTranslator translator) {
        if (AbstractLogger.getRecursionDepth() > 1) { // LOG4J2-1518, LOG4J2-2031
            // If queue is full AND we are in a recursive call, call appender directly to prevent deadlock
            AsyncQueueFullMessageUtil.logWarningToStatusLogger();
            logMessageInCurrentThread(
                    translator.fqcn, translator.level, translator.marker, translator.message, translator.thrown);
            translator.clear();
            return;
        }
        final EventRoute eventRoute = loggerDisruptor.getEventRoute(translator.level);
        switch (eventRoute) {
            case ENQUEUE:
                loggerDisruptor.enqueueLogMessageWhenQueueFull(translator);
                break;
            case SYNCHRONOUS:
                logMessageInCurrentThread(
                        translator.fqcn, translator.level, translator.marker, translator.message, translator.thrown);
                translator.clear();
                break;
            case DISCARD:
                translator.clear();
                break;
            default:
                throw new IllegalStateException("Unknown EventRoute " + eventRoute);
        }
    }

    private void initTranslator(
            final RingBufferLogEventTranslator translator,
            final String fqcn,
            final StackTraceElement location,
            final Level level,
            final Marker marker,
            final Message message,
            final Throwable thrown) {

        translator.setBasicValues(
                this,
                name,
                marker,
                fqcn,
                level,
                message, //
                // don't construct ThrowableProxy until required
                thrown,

                // needs shallow copy to be fast (LOG4J2-154)
                ThreadContext.getImmutableStack(), //
                location,
                CLOCK, //
                nanoClock //
        );
    }

    private void initTranslator(
            final RingBufferLogEventTranslator translator,
            final String fqcn,
            final Level level,
            final Marker marker,
            final Message message,
            final Throwable thrown) {

        translator.setBasicValues(
                this,
                name,
                marker,
                fqcn,
                level,
                message, //
                // don't construct ThrowableProxy until required
                thrown,

                // needs shallow copy to be fast (LOG4J2-154)
                ThreadContext.getImmutableStack(), //

                // location (expensive to calculate)
                calcLocationIfRequested(fqcn), //
                CLOCK, //
                nanoClock //
        );
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
     * @param fqcn fully qualified name of the caller
     * @param level level at which the caller wants to log the message
     * @param marker message marker
     * @param message the log message
     * @param thrown a {@code Throwable} or {@code null}
     */
    private void logWithVarargTranslator(
            final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {

        final Disruptor<RingBufferLogEvent> disruptor = loggerDisruptor.getDisruptor();
        !disruptor
                .getRingBuffer()
                .tryPublishEvent(
                        this,
                        this, // asyncLogger: 0
                        fqcn, // 2
                        level, // 3
                        marker, // 4
                        message, // 5
                        thrown)) { // 6
            handleRingBufferFull(location, fqcn, level, marker, message, thrown);
        }
    }

    /**
     * Enqueues the specified log event data for logging in a background thread.
     * <p>
     * This creates a new varargs Object array for each invocation, but does not store any non-JDK classes in a
     * {@code ThreadLocal} to avoid memory leaks in web applications (see LOG4J2-1172).
     *
     * @param fqcn fully qualified name of the caller
     * @param location location of the caller.
     * @param level level at which the caller wants to log the message
     * @param marker message marker
     * @param message the log message
     * @param thrown a {@code Throwable} or {@code null}
     */
    private void logWithVarargTranslator(
            final String fqcn,
            final StackTraceElement location,
            final Level level,
            final Marker marker,
            final Message message,
            final Throwable thrown) {
        // Implementation note: candidate for optimization: exceeds 35 bytecodes.

        final Disruptor<RingBufferLogEvent> disruptor = loggerDisruptor.getDisruptor();
        if (disruptor == null) {
            LOGGER.error("Ignoring log event after Log4j has been shut down.");
            return;
        }
        // if the Message instance is reused, there is no point in freezing its message here
        if (!isReused(message)) {
            InternalAsyncUtil.makeMessageImmutable(message);
        }
        // calls the translateTo method on this AsyncLogger
        if (!disruptor
                .getRingBuffer()
                .tryPublishEvent(
                        this, this, // asyncLogger: 0
                        location, // location: 1
                        fqcn, // 2
                        level, // 3
                        marker, // 4
                        message, // 5
                        thrown)) { // 6
            handleRingBufferFull(location, fqcn, level, marker, message, thrown);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.lmax.disruptor.EventTranslatorVararg#translateTo(java.lang.Object, long, java.lang.Object[])
     */
    @Override
    public void translateTo(final RingBufferLogEvent event, final long sequence, final Object... args) {
        // Implementation note: candidate for optimization: exceeds 35 bytecodes.
        final AsyncLogger asyncLogger = (AsyncLogger) args[0];
        final StackTraceElement location = (StackTraceElement) args[1];
        final String fqcn = (String) args[2];
        final Level level = (Level) args[3];
        final Marker marker = (Marker) args[4];
        final Message message = (Message) args[5];
        final Throwable thrown = (Throwable) args[6];

        // needs shallow copy to be fast (LOG4J2-154)
        final ContextStack contextStack = ThreadContext.getImmutableStack();

        final Thread currentThread = Thread.currentThread();
        final String threadName = THREAD_NAME_CACHING_STRATEGY.getThreadName();
        event.setValues(
                asyncLogger,
                asyncLogger.getName(),
                marker,
                fqcn,
                level,
                message,
                thrown,
                // config properties are taken care of in the EventHandler thread
                // in the AsyncLogger#actualAsyncLog method
                CONTEXT_DATA_INJECTOR.injectContextData(null, (StringMap) event.getContextData()),
                contextStack,
                currentThread.getId(),
                threadName,
                currentThread.getPriority(),
                location,
                CLOCK,
                nanoClock);
    }

    /**
     * LOG4J2-471: prevent deadlock when RingBuffer is full and object being logged calls Logger.log() from its
     * toString() method
     *
     * @param fqcn fully qualified caller name
     * @param level log level
     * @param marker optional marker
     * @param message log message
     * @param thrown optional exception
     */
    void logMessageInCurrentThread(
            final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {
        // bypass RingBuffer and invoke Appender directly
        final ReliabilityStrategy strategy = privateConfig.loggerConfig.getReliabilityStrategy();
        strategy.log(this, getName(), fqcn, marker, level, message, thrown);
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
            logMessageInCurrentThread(fqcn, level, marker, msg, thrown);
            return;
        }
        final EventRoute eventRoute = loggerDisruptor.getEventRoute(level);
        switch (eventRoute) {
            case ENQUEUE:
                loggerDisruptor.enqueueLogMessageWhenQueueFull(
                        this, this, // asyncLogger: 0
                        location, // location: 1
                        fqcn, // 2
                        level, // 3
                        marker, // 4
                        msg, // 5
                        thrown); // 6
                break;
            case SYNCHRONOUS:
                logMessageInCurrentThread(fqcn, level, marker, msg, thrown);
                break;
            case DISCARD:
                break;
            default:
                throw new IllegalStateException("Unknown EventRoute " + eventRoute);
        }
    }

    /**
     * This method is called by the EventHandler that processes the RingBufferLogEvent in a separate thread.
     * Merges the contents of the configuration map into the contextData, after replacing any variables in the property
     * values with the StrSubstitutor-supplied actual values.
     *
     * @param event the event to log
     */
    public void actualAsyncLog(final RingBufferLogEvent event) {
        final LoggerConfig privateConfigLoggerConfig = privateConfig.loggerConfig;
        final List<Property> properties = privateConfigLoggerConfig.getPropertyList();

        if (properties != null) {
            onPropertiesPresent(event, properties);
        }

        privateConfigLoggerConfig.getReliabilityStrategy().log(this, event);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach") // Avoid iterator allocation
    private void onPropertiesPresent(final RingBufferLogEvent event, final List<Property> properties) {
        final StringMap contextData = getContextData(event);
        for (int i = 0, size = properties.size(); i < size; i++) {
            final Property prop = properties.get(i);
            if (contextData.getValue(prop.getName()) != null) {
                continue; // contextMap overrides config properties
            }
            final String value = prop.evaluate(privateConfig.config.getStrSubstitutor());
            contextData.putValue(prop.getName(), value);
        }
        event.getContextData(contextData);
    }

    private static StringMap getContextData(final RingBufferLogEvent event) {
        final StringMap contextData = (StringMap) event.getContextData();
        if (contextData.isFrozen()) {
            final StringMap temp = ContextDataFactory.createContextData();
            temp.putAll(contextData);
            return temp;
        }
        return contextData;
    }

    // package-protected for tests
    AsyncLoggerDisruptor getAsyncLoggerDisruptor() {
        return loggerDisruptor;
    }
}