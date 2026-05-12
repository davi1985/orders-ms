package tech.buildrun.btgpactual.orderms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import tech.buildrun.btgpactual.orderms.entities.Order;

public interface OrderRepository extends MongoRepository<Order, Long> {

  Page<Order> findAllByCustomerId(Long customerId, PageRequest pageRequest);
}