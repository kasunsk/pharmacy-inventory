package lk.pharmacy.inventory.config;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayDemoDataConfig {

    @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer(
            @Value("${app.demo-data.enabled:false}") boolean demoDataEnabled
    ) {
        return (FluentConfiguration configuration) -> {
            if (demoDataEnabled) {
                configuration.locations("classpath:db/migration", "classpath:db/demo");
                return;
            }
            configuration.locations("classpath:db/migration");
        };
    }
}

