package com.example.ytclone.infrastructure.config;

import org.hibernate.type.format.FormatMapper;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * Hibernate in version 7.2 search for jackson 2, but spring boot 4 has jackson 3 so it need to be configured.
 * In Hibernate v7.3 automatic jackson 3 search should be implemented so configuration could be deleted
 */
@Configuration
public class HibernateConfig {
    @Bean
    public FormatMapper jsonFormatMapper(ObjectMapper objectMapper) {
        return new Jackson3JsonFormatMapper(objectMapper);
    }

    @Bean
    public HibernatePropertiesCustomizer jsonFormatMapperCustomizer(FormatMapper jsonFormatMapper) {
        return properties -> properties.put(
                "hibernate.type.json_format_mapper",
                jsonFormatMapper
        );
    }
}
