package iuh.fit.nhom16.pcstore.advisory.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name={"app.recommendations.enabled","app.recommendations.auto-sync"},havingValue="true")
public class SemanticIndexScheduler {
    private static final Logger LOG=LoggerFactory.getLogger(SemanticIndexScheduler.class);
    private final ProductSemanticIndexService index;
    public SemanticIndexScheduler(ProductSemanticIndexService index) { this.index=index; }
    @Scheduled(initialDelayString="${app.recommendations.sync-initial-delay-ms:30000}",
            fixedDelayString="${app.recommendations.sync-delay-ms:300000}")
    public void synchronize() {
        try { index.synchronize(); }
        catch (RuntimeException e) { LOG.warn("Semantic index synchronization failed; retry on the next scheduled interval"); }
    }
}
