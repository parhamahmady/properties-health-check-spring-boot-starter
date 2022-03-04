package org.parham.configurationspellcheck.service;

import lombok.extern.slf4j.Slf4j;
import org.parham.configurationspellcheck.annotation.CriticalProperty;
import org.parham.configurationspellcheck.annotation.IgnorePropertyCheck;
import org.parham.configurationspellcheck.exception.CriticalFieldInvalidValue;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.*;
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
        final IgnorePropertyCheck ignorePropertyCheckAnn = applicationContext.findAnnotationOnBean(beanName, IgnorePropertyCheck.class);
        //todo add ignore list
        if (ignorePropertyCheckAnn != null || beanClass.getPackageName().startsWith("org.springframework"))
            return bean;
        checkFieldProperties(beanName, beanClass);
        checkConstructorBindingProperties(beanName, beanClass);
        return bean;
    }

    private void checkConstructorBindingProperties(String beanName, Class<?> beanClass) {
        ConstructorBinding constructorBindingAnn = applicationContext.findAnnotationOnBean(beanName, ConstructorBinding.class);
        ConfigurationProperties configurationPropertiesAnn = applicationContext.findAnnotationOnBean(beanName, ConfigurationProperties.class);
        if (configurationPropertiesAnn == null)
            return;

        final Constructor<?>[] constructors = beanClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructorBindingAnn == null) {
                constructorBindingAnn = constructor.getAnnotation(ConstructorBinding.class);
                if (constructorBindingAnn == null)
                    continue;
            }
            String prefix = Objects.requireNonNull(configurationPropertiesAnn).prefix();
            Parameter[] parameters = constructor.getParameters();
            for (Parameter parameter : parameters) {
                String propertyKey = prefix + "." + parameter.getName()
                        .replaceAll("(?<!^)([A-Z])", "-$1").toLowerCase();
                final String property = environment.getProperty(propertyKey);
                if (!StringUtils.hasLength(property)) {
                    log.warn("Constructor Parameter {} may set invalid for Bean {}, PropertyKey was: {} and Value was Null or Not Founded", parameter.getName(), beanName, propertyKey);
                }
            }
        }
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
            if (field.isAnnotationPresent(IgnorePropertyCheck.class))
                continue;

            if (!fieldHasSetter(beanClass, field))
                continue;

            if (field.isAnnotationPresent(Value.class)) {
                final Value valueAnnotation = field.getAnnotation(Value.class);
                final String propertyKey = valueAnnotation.value();
                checkPropertyFromEnvironment(beanName, field, propertyKey);
            } else if (configurationPropertiesAnnotationPresent && isFieldAutoConfigurationAllowed(field)) {
                final String fieldName = field.getName();
                final ConfigurationProperties configurationPropertiesAnn = applicationContext.findAnnotationOnBean(beanName, ConfigurationProperties.class);
                final String propertyKey = Objects.requireNonNull(configurationPropertiesAnn).prefix() + "." +
                        fieldName.replaceAll("(?<!^)([A-Z])", "-$1").toLowerCase();
                checkPropertyFromEnvironment(beanName, field, propertyKey);
            }
        }
    }

    private void checkPropertyFromEnvironment(String beanName, Field field, String propertyKey) {
        final String property = environment.getProperty(propertyKey);
        if (!StringUtils.hasLength(property)) {
            if (!field.isAnnotationPresent(CriticalProperty.class))
                log.warn("Field {} may set invalid for Bean {}, PropertyKey was: {} and Value was Null or Not Founded", field.getName(), beanName, propertyKey);
            else {
                log.error("Field {} may set invalid for Bean {}, PropertyKey was: {} and Value was Null or Not Founded", field.getName(), beanName, propertyKey);
                if (field.getAnnotation(CriticalProperty.class).throwException())
                    throw new CriticalFieldInvalidValue("Field" + field.getName() + "may set invalid for Bean" + beanName +
                            "PropertyKey was: " + propertyKey + " and Value was Null or Not Founded");
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
