package iuh.fit.nhom16.pcstore.sales.model;

import iuh.fit.nhom16.pcstore.sales.exception.OrderException;

public enum OrderStatus {
    PENDING_PAYMENT,
    PENDING_CONFIRMATION,
    CONFIRMED,
    PROCESSING,
    SHIPPING,
    DELIVERED,
    COMPLETED,
    CANCELLED,
    EXPIRED;

    public void requireTransition(OrderStatus target, boolean admin) {
        boolean allowed = switch (target) {
            case CANCELLED -> this == PENDING_PAYMENT || this == PENDING_CONFIRMATION
                    || (admin && (this == CONFIRMED || this == PROCESSING));
            case CONFIRMED -> admin && this == PENDING_CONFIRMATION;
            case PROCESSING -> admin && this == CONFIRMED;
            case SHIPPING -> admin && (this == CONFIRMED || this == PROCESSING);
            case DELIVERED -> admin && this == SHIPPING;
            case COMPLETED -> admin && (this == SHIPPING || this == DELIVERED);
            default -> false;
        };
        if (!allowed) {
            throw OrderException.conflict("INVALID_ORDER_STATE",
                    "Không thể chuyển trạng thái đơn hàng từ " + this + " sang " + target + ".");
        }
    }
}
