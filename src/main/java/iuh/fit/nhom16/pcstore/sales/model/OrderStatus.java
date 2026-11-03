package iuh.fit.nhom16.pcstore.sales.model;

import iuh.fit.nhom16.pcstore.sales.exception.OrderException;

public enum OrderStatus {
    PENDING_PAYMENT, PENDING_CONFIRMATION, CONFIRMED, SHIPPING, COMPLETED, CANCELLED, EXPIRED;

    public void requireTransition(OrderStatus target, boolean admin) {
        boolean allowed = switch (target) {
            case CANCELLED -> this == PENDING_PAYMENT || this == PENDING_CONFIRMATION;
            case CONFIRMED -> admin && this == PENDING_CONFIRMATION;
            case SHIPPING -> admin && this == CONFIRMED;
            case COMPLETED -> admin && this == SHIPPING;
            default -> false;
        };
        if (!allowed) throw OrderException.conflict("INVALID_ORDER_STATE", "Không thể chuyển trạng thái đơn hàng.");
    }
}
