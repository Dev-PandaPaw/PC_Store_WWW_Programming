package iuh.fit.nhom16.pcstore.sales.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.Clock;

@Configuration
@EnableAsync
@EnableScheduling
public class OrderConfiguration {
    @Bean public Clock orderClock() { return Clock.systemUTC(); }
}
