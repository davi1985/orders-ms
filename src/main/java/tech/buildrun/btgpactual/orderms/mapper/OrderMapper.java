package tech.buildrun.btgpactual.orderms.mapper;

import java.math.BigDecimal;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import tech.buildrun.btgpactual.orderms.entities.Order;
import tech.buildrun.btgpactual.orderms.entities.OrderItem;
import tech.buildrun.btgpactual.orderms.listener.dto.OrderCreatedEvent;
import tech.buildrun.btgpactual.orderms.listener.dto.OrderItemEvent;

@Mapper(componentModel = "spring")
public interface OrderMapper {

  @Mapping(target = "total", expression = "java(calculateTotal(event))")
  Order toEntity(OrderCreatedEvent event);

  OrderItem toOrderItem(OrderItemEvent itemEvent);

  default BigDecimal calculateTotal(OrderCreatedEvent event) {
    return event.items()
        .stream()
        .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
        .reduce(BigDecimal::add)
        .orElse(BigDecimal.ZERO);
  }
}
