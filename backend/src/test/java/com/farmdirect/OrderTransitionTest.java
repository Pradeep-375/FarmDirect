package com.farmdirect;

import com.farmdirect.enums.OrderStatus;
import com.farmdirect.service.OrderStatusService;
import org.junit.jupiter.api.Test;
import static com.farmdirect.enums.OrderStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class OrderTransitionTest {
  @Test void happyPath() {
    OrderStatus[] path = {ORDER_PLACED, FARMER_ACCEPTED, PROCESSING, READY_FOR_PICKUP, PICKED_UP, OUT_FOR_DELIVERY, DELIVERED};
    for (int i = 0; i < path.length - 1; i++) assertTrue(OrderStatusService.canMove(path[i], path[i + 1]), path[i] + " -> " + path[i + 1]);
  }
  @Test void cannotSkipOrGoBack() {
    assertFalse(OrderStatusService.canMove(ORDER_PLACED, DELIVERED));
    assertFalse(OrderStatusService.canMove(PICKED_UP, PROCESSING));
    assertFalse(OrderStatusService.canMove(CANCELLED, ORDER_PLACED));
    assertFalse(OrderStatusService.canMove(OUT_FOR_DELIVERY, CANCELLED));
  }
  @Test void customerCanCancelOnlyEarly() {
    assertTrue(OrderStatusService.canMove(ORDER_PLACED, CANCELLED)); assertTrue(OrderStatusService.canMove(FARMER_ACCEPTED, CANCELLED));
    assertFalse(OrderStatusService.canMove(READY_FOR_PICKUP, CANCELLED));
  }
}
