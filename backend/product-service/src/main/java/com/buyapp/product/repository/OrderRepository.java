package com.buyapp.product.repository;

import com.buyapp.product.entity.Order;
import com.buyapp.product.entity.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByBuyerIdOrderByCreatedAtDesc(String buyerId);

    List<Order> findByBuyerIdAndStatusOrderByCreatedAtDesc(String buyerId, OrderStatus status);

    @Query("{'items.sellerId': ?0}")
    List<Order> findOrdersContainingSeller(String sellerId);
}
