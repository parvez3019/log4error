package io.github.parvez3019.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Plugin(
        name = "Log4ErrorAppender",
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE,
        printObject = true
)
public final class Log4ErrorAppender extends AbstractAppender {
    private static final int DEFAULT_BUFFER_SIZE = 500;

    private static final org.apache.logging.log4j.Logger STATUS_LOGGER =
            LogManager.getLogger(Log4ErrorAppender.class);

    private final List<Appender> delegates;
    private final int bufferSize;
    private final ThreadLocal<ArrayDeque<LogEvent>> buffers =
            ThreadLocal.withInitial(ArrayDeque::new);

    public Log4ErrorAppender(String name, int bufferSize, List<Appender> delegates) {
        super(name, null, null, true, Property.EMPTY_ARRAY);
        if (bufferSize < 1) {
            throw new IllegalArgumentException("bufferSize must be at least 1");
        }
        if (delegates == null || delegates.isEmpty()) {
            throw new IllegalArgumentException("At least one delegate appender is required");
        }
        this.bufferSize = bufferSize;
        this.delegates = List.copyOf(delegates);
    }

    @PluginFactory
    public static Log4ErrorAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginAttribute(value = "bufferSize", defaultInt = DEFAULT_BUFFER_SIZE) int bufferSize,
            @PluginElement("AppenderRef") AppenderRef[] appenderRefs,
            @PluginConfiguration Configuration configuration) {
        if (name == null || name.isBlank()) {
            STATUS_LOGGER.error("Log4ErrorAppender requires a non-empty name");
            return null;
        }
        if (bufferSize < 1) {
            STATUS_LOGGER.error("Log4ErrorAppender bufferSize must be at least 1");
            return null;
        }
        if (appenderRefs == null || appenderRefs.length == 0) {
            STATUS_LOGGER.error("Log4ErrorAppender requires at least one nested AppenderRef");
            return null;
        }

        List<Appender> delegates = new ArrayList<>();
        for (AppenderRef reference : appenderRefs) {
            Appender delegate = configuration.getAppender(reference.getRef());
            if (delegate == null) {
                STATUS_LOGGER.error("Log4ErrorAppender could not find delegate appender '{}'",
                        reference.getRef());
                return null;
            }
            delegates.add(delegate);
        }
        return new Log4ErrorAppender(name, bufferSize, delegates);
    }

    @Override
    public void append(LogEvent event) {
        Level level = event.getLevel();
        if (Level.INFO.equals(level) || Level.DEBUG.equals(level)) {
            buffer(event.toImmutable());
            return;
        }

        if (level.isMoreSpecificThan(Level.ERROR)) {
            flushThreadBuffer();
        }
        dispatch(event.toImmutable());
    }

    public void clearThreadBuffer() {
        buffers.remove();
    }

    public int bufferedSize() {
        return buffers.get().size();
    }

    private void buffer(LogEvent event) {
        ArrayDeque<LogEvent> buffer = buffers.get();
        if (buffer.size() == bufferSize) {
            buffer.removeFirst();
        }
        buffer.addLast(event);
    }

    private void flushThreadBuffer() {
        ArrayDeque<LogEvent> buffer = buffers.get();
        while (!buffer.isEmpty()) {
            dispatch(buffer.removeFirst());
        }
    }

    private void dispatch(LogEvent event) {
        for (Appender delegate : delegates) {
            delegate.append(event);
        }
    }
}
