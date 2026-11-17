package io.github.parvez3019.spring;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.github.parvez3019.Logger;
import io.github.parvez3019.spring.support.LogCapture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Log4ErrorFilterIT.TestApp.class)
@AutoConfigureMockMvc
class Log4ErrorFilterIT {

    @Autowired
    private MockMvc mockMvc;

    private LogCapture capture;

    @BeforeEach
    void attach() {
        capture = LogCapture.attach(Logger.class);
    }

    @AfterEach
    void detach() {
        capture.close();
        RequestLogger.clear();
    }

    @Test
    void happyPathWritesNothing() throws Exception {
        mockMvc.perform(get("/orders/1")).andExpect(status().isOk());

        assertThat(capture.events()).isEmpty();
        assertThat(RequestLogger.isBound()).isFalse();
    }

    @Test
    void errorFlushesBufferedContextThenError() throws Exception {
        mockMvc.perform(get("/orders/fail").header("x-request-id", "req-42"))
                .andExpect(status().isOk());

        List<ILoggingEvent> events = capture.events();
        assertThat(events).extracting(ILoggingEvent::getFormattedMessage).containsExactly(
                "Fetching order fail",
                "Calling payment provider",
                "Payment failed for order fail");
        assertThat(events).extracting(ILoggingEvent::getLevel)
                .containsExactly(Level.INFO, Level.DEBUG, Level.ERROR);
        assertThat(events.get(2).getMDCPropertyMap()).containsEntry("x-request-id", "req-42");
        assertThat(RequestLogger.isBound()).isFalse();
        assertThat(MDC.get("x-request-id")).isNull();
    }

    @Test
    void generatesRequestIdWhenHeaderMissing() throws Exception {
        mockMvc.perform(get("/request-id"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.matchesPattern("[0-9a-f-]{36}")));
    }

    @Test
    void eachRequestGetsAFreshBuffer() throws Exception {
        mockMvc.perform(get("/orders/1")).andExpect(status().isOk());
        mockMvc.perform(get("/buffered-size")).andExpect(content().string("0"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(OrderController.class)
    static class TestApp {
    }

    @RestController
    static class OrderController {

        @GetMapping("/orders/{id}")
        String order(@PathVariable String id) {
            RequestLogger.get().info("Fetching order {}", id);
            RequestLogger.get().debug("Calling payment provider");
            if ("fail".equals(id)) {
                RequestLogger.get().error("Payment failed for order {}", id);
                return "failed";
            }
            return "ok";
        }

        @GetMapping("/request-id")
        String requestId() {
            return MDC.get("x-request-id");
        }

        @GetMapping("/buffered-size")
        String bufferedSize() {
            return String.valueOf(RequestLogger.get().bufferedSize());
        }
    }
}
