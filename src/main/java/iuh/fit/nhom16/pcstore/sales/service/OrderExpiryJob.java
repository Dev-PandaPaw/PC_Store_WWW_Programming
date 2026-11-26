package iuh.fit.nhom16.pcstore.sales.service;

import iuh.fit.nhom16.pcstore.sales.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Clock;

@Component
public class OrderExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(OrderExpiryJob.class);
    private final OrderRepository repository;
    private final OrderService orders;
    private final Clock clock;
    public OrderExpiryJob(OrderRepository repository, OrderService orders, Clock clock) {
        this.repository = repository; this.orders = orders; this.clock = clock;
    }
    @Scheduled(fixedDelayString="${app.orders.expiry-delay-ms:60000}", initialDelay=60000)
    public void expire() {
        for (var id : repository.expired(clock.instant())) {
            try { orders.expire(id); }
            catch (RuntimeException ex) { log.warn("Could not expire order {} ({})", id, ex.getClass().getSimpleName()); }
        }
    }
}
