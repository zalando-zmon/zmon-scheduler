package de.zalando.zmon.scheduler.ng;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Created by jmussler on 4/7/15.
 */
@Configuration
@Primary
public class JacksonBuilder {
/*
    @Bean
    @Primary
    public Jackson2ObjectMapperBuilder objectMapperBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.propertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        builder.featuresToEnable(SerializationFeature.INDENT_OUTPUT);
        return builder;
    } */
}
