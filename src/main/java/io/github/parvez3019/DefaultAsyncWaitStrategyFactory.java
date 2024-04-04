package io.github.parvez3019;

import com.lmax.disruptor.*;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.async.AsyncWaitStrategyFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Strings;
import com.lmax.disruptor.SleepingWaitStrategy;


class DefaultAsyncWaitStrategyFactory implements AsyncWaitStrategyFactory {
    static final String DEFAULT_WAIT_STRATEGY_CLASSNAME = TimeoutBlockingWaitStrategy.class.getName();
    private static final Logger LOGGER = StatusLogger.getLogger();
    private final String propertyName;

    public DefaultAsyncWaitStrategyFactory(final String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public WaitStrategy createWaitStrategy() {
        final String strategy = PropertiesUtil.getProperties().getStringProperty(propertyName, "TIMEOUT");
        LOGGER.trace("DefaultAsyncWaitStrategyFactory property {}={}", propertyName, strategy);
        final String strategyUp = Strings.toRootUpperCase(strategy);
        // String (not enum) is deliberately used here to avoid IllegalArgumentException being thrown. In case of
        // incorrect property value, default WaitStrategy is created.
        switch (strategyUp) {
            case "SLEEP":
                final long sleepTimeNs = parseAdditionalLongProperty(propertyName, "SleepTimeNs", 100L);
                final String key = getFullPropertyKey(propertyName, "Retries");
                final int retries = PropertiesUtil.getProperties().getIntegerProperty(key, 200);
                LOGGER.trace("DefaultAsyncWaitStrategyFactory creating SleepingWaitStrategy(retries={}, sleepTimeNs={})", retries, sleepTimeNs);
                return new SleepingWaitStrategy();
            case "YIELD":
                LOGGER.trace("DefaultAsyncWaitStrategyFactory creating YieldingWaitStrategy");
                return new YieldingWaitStrategy();
            case "BLOCK":
                LOGGER.trace("DefaultAsyncWaitStrategyFactory creating BlockingWaitStrategy");
                return new BlockingWaitStrategy();
            case "BUSYSPIN":
                LOGGER.trace("DefaultAsyncWaitStrategyFactory creating BusySpinWaitStrategy");
                return new BusySpinWaitStrategy();
            case "TIMEOUT":
                return createDefaultWaitStrategy(propertyName);
            default:
                return createDefaultWaitStrategy(propertyName);
        }
    }

    static WaitStrategy createDefaultWaitStrategy(final String propertyName) {
        final long timeoutMillis = parseAdditionalLongProperty(propertyName, "Timeout", 10L);
        LOGGER.trace("DefaultAsyncWaitStrategyFactory creating TimeoutBlockingWaitStrategy(timeout={}, unit=MILLIS)", timeoutMillis);
        // Check for the v 4.x version of the strategy, the version in 3.x is not garbage-free.
        if (DisruptorUtil.DISRUPTOR_MAJOR_VERSION == 4) {
            try {
                return (WaitStrategy) Class.forName("com.lmax.disruptor.TimeoutBlockingWaitStrategy").getConstructor(long.class, TimeUnit.class).newInstance(timeoutMillis, TimeUnit.MILLISECONDS);
            } catch (final ReflectiveOperationException | LinkageError e) {
                LOGGER.debug("DefaultAsyncWaitStrategyFactory failed to load 'com.lmax.disruptor.TimeoutBlockingWaitStrategy', using '{}' instead.", TimeoutBlockingWaitStrategy.class.getName());
            }
        }
        // Use our version
        return new TimeoutBlockingWaitStrategy(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    private static String getFullPropertyKey(final String strategyKey, final String additionalKey) {
        if (strategyKey.startsWith("AsyncLogger.")) {
            return "AsyncLogger." + additionalKey;
        } else if (strategyKey.startsWith("AsyncLoggerConfig.")) {
            return "AsyncLoggerConfig." + additionalKey;
        }
        return strategyKey + additionalKey;
    }

    private static long parseAdditionalLongProperty(final String propertyName, final String additionalKey, final long defaultValue) {
        final String key = getFullPropertyKey(propertyName, additionalKey);
        return PropertiesUtil.getProperties().getLongProperty(key, defaultValue);
    }
}
