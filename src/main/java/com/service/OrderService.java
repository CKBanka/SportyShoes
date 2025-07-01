package com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bean.*;
import com.repository.OrderRepository;
import com.repository.OrderItemRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private ProductService productService;
    
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }
    
    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUser(user);
    }
    
    public List<Order> getUserOrderHistory(Long userId) {
        return orderRepository.findUserOrderHistory(userId);
    }
    
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
    
    public List<Order> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findOrdersByDateRange(startDate, endDate);
    }
    
    public List<Order> getOrdersByCategoryAndDateRange(Long categoryId, 
                                                      LocalDateTime startDate, 
                                                      LocalDateTime endDate) {
        return orderRepository.findOrdersByCategoryAndDateRange(categoryId, startDate, endDate);
    }
    
    public List<Order> getRecentOrders(int limit) {
        List<Order> allOrders = orderRepository.findAll();
        return allOrders.stream()
                .sorted((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()))
                .limit(limit)
                .toList();
    }
    
    public Order createOrder(User user, List<OrderItem> orderItems, String shippingAddress, String paymentMethod) {
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(shippingAddress);
        order.setPaymentMethod(paymentMethod);
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        order = orderRepository.save(order);
        
        for (OrderItem item : orderItems) {
            item.setOrder(order);
            
            if (!productService.isProductAvailable(item.getProduct().getId(), item.getQuantity())) {
                throw new RuntimeException("Product " + item.getProduct().getName() + " is out of stock");
            }
            
            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
            
            productService.updateStock(item.getProduct().getId(), item.getQuantity());
            
            orderItemRepository.save(item);
        }
        
        // Update order total
        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);
        
        return orderRepository.save(order);
    }
    
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = getOrderById(orderId);
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(status);
        
        // If order is cancelled, restore stock
        if (status == OrderStatus.CANCELLED && oldStatus != OrderStatus.CANCELLED) {
            for (OrderItem item : order.getOrderItems()) {
                productService.restoreStock(item.getProduct().getId(), item.getQuantity());
            }
        }
        
        return orderRepository.save(order);
    }
    
    public void cancelOrder(Long orderId) {
        updateOrderStatus(orderId, OrderStatus.CANCELLED);
    }
    
    public long getTotalOrderCount() {
        return orderRepository.count();
    }
    
    public BigDecimal getTotalRevenue() {
        List<Order> completedOrders = orderRepository.findByStatus(OrderStatus.DELIVERED);
        return completedOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public BigDecimal getRevenueByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders = getOrdersByDateRange(startDate, endDate);
        return orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}