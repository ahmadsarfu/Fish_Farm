package com.fishfarm.management.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.validation.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.*;
import java.time.*;
@Configuration
public class BusinessTimeConfig {
    @Bean Clock businessClock(@Value("${app.business-zone:Asia/Kolkata}") String zone){return Clock.system(ZoneId.of(zone));}
    @Bean ValidationConfigurationCustomizer businessValidationClock(Clock clock){return configuration->configuration.clockProvider(()->clock);}
}
