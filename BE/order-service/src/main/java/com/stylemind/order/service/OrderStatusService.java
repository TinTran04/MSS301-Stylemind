package com.stylemind.order.service;

import com.stylemind.order.entity.Order;
import com.stylemind.order.entity.OrderStatus;

public interface OrderStatusService {

    Order changeStatus(String orderId, OrderStatus target, String actorId);

    Order changeStatus(Order order, OrderStatus target, String actorId);
}
