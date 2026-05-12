package tech.buildrun.btgpactual.orderms.controller;

import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tech.buildrun.btgpactual.orderms.controller.dto.ApiResponse;
import tech.buildrun.btgpactual.orderms.controller.dto.OrderResponse;
import tech.buildrun.btgpactual.orderms.controller.dto.PaginationResponse;
import tech.buildrun.btgpactual.orderms.service.OrderService;

@RestController
public class OrderController {

  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @GetMapping("/customers/{customerId}/orders")
  public ResponseEntity<ApiResponse<OrderResponse>> listOrders(
      @PathVariable("customerId") Long customId,
      @RequestParam(name = "page", defaultValue = "0") Integer page,
      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {

    var response = orderService.findAllByCustomerId(
        customId,
        PageRequest.of(page, pageSize));

    var totalOrders = orderService.findTotalOnOrdersByCustomerId(customId);

    return ResponseEntity.ok(new ApiResponse<>(
        Map.of("totalOrders", totalOrders),
        response.getContent(),
        PaginationResponse.fromPage(response)));
  }
}
