package io.github.parvez3019.spring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class Log4ErrorAutoConfigurationTest {

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(Log4ErrorAutoConfiguration.class));

    @Test
    void registersFilterByDefault() {
        webRunner.run(context -> {
            assertThat(context).hasSingleBean(Log4ErrorFilter.class);
            assertThat(context).hasBean("log4ErrorFilterRegistration");
            FilterRegistrationBean<?> registration =
                    context.getBean("log4ErrorFilterRegistration", FilterRegistrationBean.class);
            assertThat(registration.getOrder())
                    .isEqualTo(new Log4ErrorProperties().getFilterOrder());
        });
    }

    @Test
    void bindsProperties() {
        webRunner.withPropertyValues(
                        "log4error.buffer-size=42",
                        "log4error.request-id-header=x-trace-id",
                        "log4error.mdc-key=traceId",
                        "log4error.filter-order=7")
                .run(context -> {
                    Log4ErrorProperties properties = context.getBean(Log4ErrorProperties.class);
                    assertThat(properties.getBufferSize()).isEqualTo(42);
                    assertThat(properties.getRequestIdHeader()).isEqualTo("x-trace-id");
                    assertThat(properties.getMdcKey()).isEqualTo("traceId");
                    assertThat(context.getBean("log4ErrorFilterRegistration", FilterRegistrationBean.class)
                            .getOrder()).isEqualTo(7);
                });
    }

    @Test
    void backsOffWhenDisabled() {
        webRunner.withPropertyValues("log4error.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(Log4ErrorFilter.class);
                    assertThat(context).doesNotHaveBean("log4ErrorFilterRegistration");
                });
    }

    @Test
    void backsOffOutsideServletApps() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(Log4ErrorAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(Log4ErrorFilter.class));
    }

    @Test
    void userFilterBeanWins() {
        webRunner.withUserConfiguration(CustomFilterConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(Log4ErrorFilter.class);
                    assertThat(context.getBean(Log4ErrorFilter.class))
                            .isSameAs(context.getBean("customFilter"));
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomFilterConfig {
        @Bean
        Log4ErrorFilter customFilter() {
            return new Log4ErrorFilter(new Log4ErrorProperties());
        }
    }
}
