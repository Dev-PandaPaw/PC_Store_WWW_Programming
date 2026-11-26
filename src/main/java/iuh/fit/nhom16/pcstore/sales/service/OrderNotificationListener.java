package iuh.fit.nhom16.pcstore.sales.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(OrderNotificationListener.class);
    private final ObjectProvider<JavaMailSender> sender;
    public OrderNotificationListener(ObjectProvider<JavaMailSender> sender) { this.sender = sender; }
    @Async
    @TransactionalEventListener
    public void send(OrderNotification event) {
        if (event.email() == null || event.email().isBlank()) return;
        try {
            JavaMailSender mail = sender.getIfAvailable();
            if (mail == null) return;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("orders@pcstore.vn"); message.setTo(event.email());
            message.setSubject("PC Store - " + event.code());
            message.setText("Đơn hàng " + event.code() + " có trạng thái: " + event.status());
            mail.send(message);
        } catch (Exception ex) {
            // Do not log customer PII or SMTP credentials. The order has already committed.
            log.warn("Order email failed for {} ({})", event.code(), ex.getClass().getSimpleName());
        }
    }
}
