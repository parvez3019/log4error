package io.github.parvez3019.spring;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Registers {@link Log4ErrorFilter} in servlet web applications.
 * Disable with {@code log4error.enabled=false}; replace by declaring your own {@link Log4ErrorFilter} bean.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(OncePerRequestFilter.class)
@ConditionalOnProperty(prefix = "log4error", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(Log4ErrorProperties.class)
public class Log4ErrorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Log4ErrorFilter log4ErrorFilter(Log4ErrorProperties properties) {
        return new Log4ErrorFilter(properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "log4ErrorFilterRegistration")
    public FilterRegistrationBean<Log4ErrorFilter> log4ErrorFilterRegistration(
            Log4ErrorFilter filter, Log4ErrorProperties properties) {
        FilterRegistrationBean<Log4ErrorFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setName("log4ErrorFilter");
        registration.setOrder(properties.getFilterOrder());
        return registration;
    }
}
