package com.tproject.workshop.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

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
