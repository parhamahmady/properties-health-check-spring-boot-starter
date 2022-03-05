
# properties-health-check-spring-boot-starter

An Spring Boot Starter for checking every properties that springboot set of
fields or constructor parameters;

This stater checks every classes that annotated with @ConfigurationProperties , @ConstructorBindig or
classes that use @Value on their fields. and will check whether the property keys are available or not.



## Usage

* Add maven dependecy

```
        <dependency>
            <groupId>org.parham</groupId>
            <artifactId>properties-health-check-spring-boot-starter</artifactId>
            <version>${version}<version>
        </dependency>

```

>By adding this depecndecy, the starter will run automatically every time you start the machine.\
To disable auto starting , just exclude PropertiesHealthCheckStarterConfig.class from your SpringBoot application.


* Customization
```
 Use @IgnorePropertyCheck on bean creation method or the specific field or specific class 
 to ignore that element while properties check.
```

```
 Use @CriticalProperty on an specific field to log on error level for that element.
 also you can use @CriticalProperty(throwException = true) to avoid application from starting.
```

* Properties

```
properties.health-check.log-not-found-properties = true/false 
If false the starter won't log any non critical fields.
Default is true.
```
```
properties.health-check.ignore-packages= exmaple.package1,example.package2.*
By using this you can igore a package and its subs.
Package org.springframework.* will ignore by default.  
```
