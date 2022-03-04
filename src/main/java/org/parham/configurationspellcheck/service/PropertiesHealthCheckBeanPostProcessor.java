package org.parham.configurationspellcheck.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

/**
 * @author Parham Ahmady
 * @since 4/4/2022
 */
@Slf4j
public class PropertiesHealthCheckBeanPostProcessor
        implements BeanPostProcessor, ApplicationContextAware, EnvironmentAware, PriorityOrdered {
    private ApplicationContext applicationContext;
    private Environment environment;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        final Class<?> beanClass = bean.getClass();
        //todo add ignore list
        if (beanClass.getPackageName().startsWith("org.springframework"))
            return bean;
        checkFieldProperties(beanName, beanClass);
        return bean;
    }

    private void checkFieldProperties(String beanName, Class<?> beanClass) {
        final Field[] beanClassFields = beanClass.getDeclaredFields();
        boolean configurationPropertiesAnnotationPresent;
        try {
            configurationPropertiesAnnotationPresent = hasConfigurationPropertiesAnnotation(beanName);
        } catch (NoSuchBeanDefinitionException exception) {
            return;
        }

        for (Field field : beanClassFields) {
            //todo check ignore
            if (!fieldHasSetter(beanClass, field))
                continue;
            if (field.isAnnotationPresent(Value.class)) {
                final Value valueAnnotation = field.getAnnotation(Value.class);
                final String propertyKey = valueAnnotation.value();
                final String property = environment.getProperty(propertyKey);
                if (!StringUtils.hasLength(property))
                    log.warn("Field {} may set invalid for Bean {}, PropertyKey was: {} and Value was Null or Not Founded", field.getName(), beanName, propertyKey);
                //todo add error for critical properties
                //todo add option to log (@Critical) and  throwing
            } else if (configurationPropertiesAnnotationPresent && isFieldAutoConfigurationAllowed(field)) {
                final String fieldName = field.getName();
                final ConfigurationProperties configurationPropertiesAnn = applicationContext.findAnnotationOnBean(beanName, ConfigurationProperties.class);
                final String propertyKey = Objects.requireNonNull(configurationPropertiesAnn).prefix() + "." +
                        fieldName.replaceAll("(?<!^)([A-Z])", "-$1").toLowerCase();
                final String property = environment.getProperty(propertyKey);
                if (!StringUtils.hasLength(property)) {
                    log.warn("Field {} may set invalid for Bean {}, PropertyKey was: {} and Value was Null or Not Founded", fieldName, beanName, propertyKey);
                }
                //todo add error for critical properties
                //todo add option to log (@Critical) and  throwing
            }
        }
    }

    private boolean isFieldAutoConfigurationAllowed(Field field) {
        if (field == null)
            return false;

        final Class<?> type = field.getType();
        if (ClassUtils.isPrimitiveOrWrapper(type))
            return true;
        if (type.equals(String.class))
            return true;
        if (type.isArray()) {
            if (ClassUtils.isPrimitiveArray(type) || ClassUtils.isPrimitiveWrapperArray(type)) {
                return true;
            }
            return hasConfigurationPropertiesConverter(type);
        }//todo add Map and List
        return hasConfigurationPropertiesConverter(type);
    }

    private boolean fieldHasSetter(Class<?> clazz, Field field) {
        String setterName = "set" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
        return !Modifier.isPrivate(field.getModifiers()) || ClassUtils.hasAtLeastOneMethodWithName(clazz, setterName);
    }

    private boolean hasConfigurationPropertiesConverter(Class<?> type) {
        final Map<String, Object> convertersMap = applicationContext.getBeansWithAnnotation(ConfigurationPropertiesBinding.class);
        for (String key : convertersMap.keySet()) {
            final Object c = convertersMap.get(key);
            final ParameterizedType[] genericInterfaces = (ParameterizedType[]) c.getClass().getGenericInterfaces();
            for (ParameterizedType genericInterface : genericInterfaces) {
                if (genericInterface.getRawType().getTypeName().equals(Converter.class.getName())) {
                    final Type[] actualTypeArguments = genericInterface.getActualTypeArguments();
                    for (Type actualTypeArgument : actualTypeArguments) {
                        if (actualTypeArgument.getClass().getName().equals(type.getComponentType().getName()))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasConfigurationPropertiesAnnotation(String beanName) {
        final ConfigurationProperties configurationPropertiesAnn = applicationContext.findAnnotationOnBean(beanName, ConfigurationProperties.class);
        return configurationPropertiesAnn != null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
