package tech.buildrun.btgpactual.orderms.listener.dto;

import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long customerId,
        List<OrderItemEvent> items) {
}
