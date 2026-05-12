package tech.buildrun.btgpactual.orderms.controller.dto;

import java.math.BigDecimal;

import tech.buildrun.btgpactual.orderms.entities.Order;

public record OrderResponse(
    Long orderId,
    Long customerId,
    BigDecimal total) {

  public static OrderResponse fromEntity(Order entity) {
    return new OrderResponse(
        entity.getOrderId(),
        entity.getCustomerId(),
        entity.getTotal());
  }
}