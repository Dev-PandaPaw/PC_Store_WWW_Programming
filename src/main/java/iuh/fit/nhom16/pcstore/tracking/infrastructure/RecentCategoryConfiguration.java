package iuh.fit.nhom16.pcstore.tracking.infrastructure;

import iuh.fit.nhom16.pcstore.advisory.api.RecentCategoryProvider;
import iuh.fit.nhom16.pcstore.tracking.service.UserTrackingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods=false)
public class RecentCategoryConfiguration {
    @Bean
    @ConditionalOnProperty(name="app.recommendations.behavior-source",havingValue="tracking",matchIfMissing=true)
    RecentCategoryProvider recentCategoryProvider(UserTrackingService history) {
        return history::getTopViewedCategories;
    }
}
