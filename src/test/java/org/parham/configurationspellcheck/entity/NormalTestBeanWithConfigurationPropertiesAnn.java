package org.parham.configurationspellcheck.entity;

import lombok.Data;
import org.parham.configurationspellcheck.annotation.IgnorePropertyCheck;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * @author Parham Ahmady
 * @since 3/4/2022
 */
@Component
@ConfigurationProperties("test")
@Data

public class NormalTestBeanWithConfigurationPropertiesAnn {
    @IgnorePropertyCheck
    private String studentName;
    private int[] scores;
    private Integer id;
}
