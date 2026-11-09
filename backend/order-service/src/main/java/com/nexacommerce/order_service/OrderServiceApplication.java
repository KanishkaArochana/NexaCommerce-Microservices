package com.nexacommerce.order_service;

import com.nexacommerce.order_service.domain.model.Order;
import com.nexacommerce.order_service.domain.model.OrderItem;
import com.nexacommerce.order_service.infrastructure.persistence.OrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}

	@Bean
	CommandLineRunner testRelationship(OrderRepository orderRepository) {
		return args -> {

			// 1. Create a new Order
			Order order = new Order(1001L);

			// 2. Create OrderItems and link them to the Order
			OrderItem item1 = new OrderItem(501L, 2, new BigDecimal("2500.00"));
			item1.setOrder(order);
			order.getItems().add(item1);

			OrderItem item2 = new OrderItem(502L, 1, new BigDecimal("1200.00"));
			item2.setOrder(order);
			order.getItems().add(item2);

			// 3. Save the Order (CascadeType.ALL will automatically save both OrderItems)
			Order savedOrder = orderRepository.save(order);

			// 4. Verify saved data
			System.out.println("SUCCESS: Order Saved with ID: " + savedOrder.getId());
			System.out.println("Total Items Saved: " + savedOrder.getItems().size());
			for (OrderItem item : savedOrder.getItems()) {
				System.out.println("   Item ID: " + item.getId() + " | Product ID: " + item.getProductId() + " | Qty: " + item.getQuantity() + " | Price: " + item.getUnitPrice());
			}
		};
	}

}
