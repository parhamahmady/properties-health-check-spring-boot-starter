package org.parham.configurationspellcheck;

import org.junit.jupiter.api.Test;
import org.parham.configurationspellcheck.configuration.ContextConfiguration;
import org.parham.configurationspellcheck.configuration.TestConfiguration;
import org.parham.configurationspellcheck.entity.NormalTestBeanWithConfigurationPropertiesAnn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

@SpringBootTest(classes = {ContextConfiguration.class, TestConfiguration.class})
class ConfigurationSpellCheckStarterApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private NormalTestBeanWithConfigurationPropertiesAnn normalTestBeanWithConfigurationPropertiesAnn;

    @Test
    void contextLoads() {
        Assert.notNull(applicationContext, "No App Context");
        Assert.notNull(normalTestBeanWithConfigurationPropertiesAnn, "No Normal Test Bean");
    }

}
