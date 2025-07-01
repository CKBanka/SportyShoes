package com.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bean.*;
import com.service.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequestMapping("/customer")
@Controller
public class CustomerController {
    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @GetMapping("/dashboard")
    public String customerDashboard(Authentication authentication, Model model) {
        try {
            System.out.println("Loading customer dashboard");
            User customer = userService.findByUsername(authentication.getName()).orElse(null);
            
            if (customer == null) {
                System.out.println("Customer not found for username: " + authentication.getName());
                return "redirect:/login";
            }

            System.out.println("Customer found: " + customer.getUsername());
            model.addAttribute("customer", customer);
            model.addAttribute("totalOrders", orderService.getUserOrderHistory(customer.getId()));
            model.addAttribute("products", productService.getAllProducts());
            return "customer/dashboard";
        } catch (Exception e) {
            System.out.println("Error in customer dashboard: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "An error occurred while loading the dashboard.");
            return "customer/dashboard";
        }
    }

    @PostMapping("/buy-now")
    public String buyNow(@RequestParam("productId") Long productId,
                        @RequestParam("quantity") int quantity,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        try {
            System.out.println("Buy now request received for product: " + productId + ", quantity: " + quantity);
            
            User customer = userService.findByUsername(authentication.getName()).orElse(null);
            if (customer == null) {
                redirectAttributes.addFlashAttribute("error", "User not found. Please login again.");
                return "redirect:/login";
            }

            Product product = productService.getProductById(productId);
            if (product == null) {
                redirectAttributes.addFlashAttribute("error", "Product not found.");
                return "redirect:/customer/dashboard";
            }


            return "redirect:/customer/payment?productId=" + productId + "&quantity=" + quantity;
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Invalid product ID or quantity.");
            return "redirect:/customer/dashboard";
        } catch (Exception e) {
            System.out.println("Error in buy now: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Please try again.");
            return "redirect:/customer/dashboard";
        }
    }

    @GetMapping("/payment")
    public String paymentPage(@RequestParam("productId") Long productId,
                             @RequestParam("quantity") int quantity,
                             Authentication authentication,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            System.out.println("Loading payment page for product: " + productId + ", quantity: " + quantity);
            
            User customer = userService.findByUsername(authentication.getName()).orElse(null);
            
            Product product = productService.getProductById(productId);
            if (product == null) {
                redirectAttributes.addFlashAttribute("error", "Product not found.");
                return "redirect:/customer/dashboard";
            }

            if (quantity <= 0 || quantity > product.getStockQuantity()) {
                redirectAttributes.addFlashAttribute("error", 
                    "Invalid quantity. Please select a valid quantity.");
                return "redirect:/customer/dashboard";
            }

            BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));

            model.addAttribute("product", product);
            model.addAttribute("quantity", quantity);
            model.addAttribute("totalAmount", totalAmount);
            model.addAttribute("customer", customer);
            System.out.println("Payment page loaded successfully for product: " + product.getName());
            return "customer/payment";
        } catch (Exception e) {
            System.out.println("Error loading payment page: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "An error occurred while loading the payment page.");
            return "redirect:/customer/dashboard";
        }
    }

    @PostMapping("/process-payment")
    public String processPayment(@RequestParam("productId") Long productId,
                                @RequestParam("quantity") int quantity,
                                @RequestParam("paymentMethod") String paymentMethod,
                                @RequestParam(value = "address", required = false) String address,
                                @RequestParam(value = "phone", required = false) String phone,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            User customer = userService.findByUsername(authentication.getName()).orElse(null);
            if (customer == null) {
                redirectAttributes.addFlashAttribute("error", "User not found. Please login again.");
                return "redirect:/login";
            }

            Product product = productService.getProductById(productId);
            if (product == null) {
                redirectAttributes.addFlashAttribute("error", "Product not found.");
                return "redirect:/customer/dashboard";
            }


            BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));

            Order order = new Order();
            order.setUser(customer);
            order.setOrderDate(LocalDateTime.now());
            order.setTotalAmount(totalAmount);
            order.setPaymentMethod(paymentMethod);
            order.setShippingAddress(address != null ? address : customer.getAddress());

            List<OrderItem> orderItems = new ArrayList<>();
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(product.getPrice());
            orderItems.add(orderItem);

            order.setOrderItems(orderItems);

            Order savedOrder = orderService.createOrder(customer,orderItems,address, paymentMethod);
            productService.updateProduct(product);

            System.out.println("Order created successfully with ID: " + savedOrder.getId());
            redirectAttributes.addFlashAttribute("success", "Order placed successfully! Order ID: " + savedOrder.getId());
            return "redirect:/customer/dashboard";

        } catch (Exception e) {
            System.out.println("Error processing payment: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", 
                "An error occurred while processing your payment. Please try again.");
            return "redirect:/customer/payment?productId=" + productId + "&quantity=" + quantity;
        }
    }
    
    
}