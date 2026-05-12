package tech.buildrun.btgpactual.orderms.service;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import tech.buildrun.btgpactual.orderms.controller.dto.OrderResponse;
import tech.buildrun.btgpactual.orderms.listener.dto.OrderCreatedEvent;
import tech.buildrun.btgpactual.orderms.mapper.OrderMapper;
import tech.buildrun.btgpactual.orderms.repository.OrderRepository;

@Service
public class OrderService {

  private final OrderRepository orderRepository;
  private final MongoTemplate mongoTemplate;
  private final OrderMapper orderMapper;

  public OrderService(
      OrderRepository orderRepository,
      MongoTemplate mongoTemplate,
      OrderMapper orderMapper) {
    this.orderRepository = orderRepository;
    this.mongoTemplate = mongoTemplate;
    this.orderMapper = orderMapper;
  }

  public void save(OrderCreatedEvent event) {
    var order = orderMapper.toEntity(event);
    orderRepository.save(order);
  }

  public Page<OrderResponse> findAllByCustomerId(
      Long customerId,
      PageRequest pageRequest) {
    var orders = orderRepository.findAllByCustomerId(customerId, pageRequest);

    return orders.map(OrderResponse::fromEntity);
  }

  public BigDecimal findTotalOnOrdersByCustomerId(Long customerId) {
    var aggregations = newAggregation(
        match(Criteria
            .where("customerId")
            .is(customerId)),
        group().sum("total").as("total"));

    var response = mongoTemplate.aggregate(
        aggregations,
        "tb_orders",
        Document.class);

    var result = response.getUniqueMappedResult();

    return result != null
        ? new BigDecimal(result.get("total").toString())
        : BigDecimal.ZERO;
  }
}
