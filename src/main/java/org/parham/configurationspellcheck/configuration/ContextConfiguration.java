package org.parham.configurationspellcheck.configuration;

import org.parham.configurationspellcheck.service.PropertiesHealthCheckBeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Parham Ahmady
 * @since 4/3/2022
 */
@Configuration
public class ContextConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PropertiesHealthCheckBeanPostProcessor propertiesHealthCheckBeanPostProcessor() {
        return new PropertiesHealthCheckBeanPostProcessor();
    }

}
