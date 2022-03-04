package org.parham.configurationspellcheck.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Parham Ahmady
 * @since 4/4/2022
 */
@Component
@ConfigurationProperties("test")
@Data
public class NormalTestBeanWithConfigurationPropertiesAnn {
    private String studentName;
    private int[] scores;
    private Integer id;
}