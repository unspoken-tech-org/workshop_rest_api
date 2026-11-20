package com.tproject.workshop.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultProfileConfigTest {

    @Test
    void shouldNotActivateLocalProfileByDefaultInApplicationYml() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties props = yaml.getObject();

        assertThat(props).isNotNull();
        assertThat(props.getProperty("spring.profiles.active")).isNull();
        assertThat(props.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(props.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
    }
}
