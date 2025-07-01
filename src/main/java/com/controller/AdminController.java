package com.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bean.*;
import com.service.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        // Dashboard statistics
        long totalProducts = productService.getTotalProductCount();
        long totalCustomers = userService.getAllCustomers().size();
        long totalOrders = orderService.getTotalOrderCount();
        BigDecimal totalRevenue = orderService.getTotalRevenue();
        
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        
        List<Order> recentOrders = orderService.getRecentOrders(5);
        model.addAttribute("recentOrders", recentOrders);
        
        return "admin/dashboard";
    }
    
    @GetMapping("/products")
    public String manageProducts(Model model, 
                               @RequestParam(value = "category", required = false) Long categoryId,
                               @RequestParam(value = "search", required = false) String search) {
        List<Product> products;
        
        if (search != null && !search.trim().isEmpty()) {
            products = productService.searchProducts(search);
        } else if (categoryId != null) {
            products = productService.getProductsByCategory(categoryId);
        } else {
            products = productService.getAllProducts();
        }
        
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("searchTerm", search);
        
        return "admin/products";
    }
    
    @GetMapping("/products/add")
    public String addProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/add-product";
    }
    
    @PostMapping("/products/add")
    public String addProduct(@ModelAttribute Product product, RedirectAttributes redirectAttributes) {
        try {
            productService.saveProduct(product);
            redirectAttributes.addFlashAttribute("success", "Product added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }
    
    @GetMapping("/products/edit/{id}")
    public String editProductForm(@PathVariable Long id, Model model) {
        try {
            Product product = productService.getProductById(id);
            model.addAttribute("product", product);
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin/edit-product";
        } catch (Exception e) {
        	System.out.println("Error fetching product for edit: " + e.getMessage());
            return "redirect:/admin/products";
        }
    }
    
    @PostMapping("/products/edit/{id}")
    public String editProduct(@PathVariable Long id, @ModelAttribute Product product, 
                            RedirectAttributes redirectAttributes) {
        try {
            product.setId(id);
            productService.updateProduct(product);
            redirectAttributes.addFlashAttribute("success", "Product updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }
    
    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("success", "Product deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }
    

    
    @GetMapping("/users")
    public String manageUsers(Model model, @RequestParam(value = "search", required = false) String search) {
        List<User> users;
        
        if (search != null && !search.trim().isEmpty()) {
            users = userService.searchUsers(search);
        } else {
            users = userService.getAllCustomers();
        }
        
        model.addAttribute("users", users);
        model.addAttribute("searchTerm", search);
        return "admin/users";
    }
    
    @GetMapping("/users/{id}/orders")
    public String viewUserOrders(@PathVariable Long id, Model model) {
        try {
            User user = userService.getUserById(id);
            List<Order> orders = orderService.getUserOrderHistory(id);
            
            model.addAttribute("user", user);
            model.addAttribute("orders", orders);
            return "admin/user-orders";
        } catch (Exception e) {
            return "redirect:/admin/users";
        }
    }
    
    @GetMapping("/orders")
    public String manageOrders(Model model,
                             @RequestParam(value = "status", required = false) String status,
                             @RequestParam(value = "startDate", required = false) 
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                             @RequestParam(value = "endDate", required = false) 
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                             @RequestParam(value = "category", required = false) Long categoryId) {
        
        List<Order> orders;
        
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            
            if (categoryId != null) {
                orders = orderService.getOrdersByCategoryAndDateRange(categoryId, start, end);
            } else {
                orders = orderService.getOrdersByDateRange(start, end);
            }
        } else if (status != null && !status.isEmpty()) {
            orders = orderService.getOrdersByStatus(OrderStatus.valueOf(status.toUpperCase()));
        } else {
            orders = orderService.getAllOrders();
        }
        
        model.addAttribute("orders", orders);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("orderStatuses", OrderStatus.values());
        
        return "admin/orders";
    }
    
    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id, 
                                  @RequestParam String status, 
                                  RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, OrderStatus.valueOf(status.toUpperCase()));
            redirectAttributes.addFlashAttribute("success", "Order status updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update order status: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @GetMapping("/profile")
    public String adminProfile(Authentication authentication, Model model) {
        User admin = userService.findByUsername(authentication.getName()).orElse(null);
        model.addAttribute("admin", admin);
        return "admin/profile";
    }
    
    @PostMapping("/change-password")
    public String changePassword(Authentication authentication,
                               @RequestParam String currentPassword,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               RedirectAttributes redirectAttributes) {
        try {
            User admin = userService.findByUsername(authentication.getName()).orElse(null);
            
            if (!passwordEncoder.matches(currentPassword, admin.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "Current password is incorrect!");
                return "redirect:/admin/profile";
            }
            
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "New passwords do not match!");
                return "redirect:/admin/profile";
            }
            
            userService.changePassword(admin.getUsername(), newPassword);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to change password: " + e.getMessage());
        }
        
        return "redirect:/admin/profile";
    }
}