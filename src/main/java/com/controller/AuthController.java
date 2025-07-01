package com.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.bean.User;
import com.service.UserService;

@Controller
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/")
    public String home() {
        return "login"; 
    }
    
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "logout", required = false) String logout,
                       Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password!");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully!");
        }
        return "login";
    }
    
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }
    
    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {
        try {
            if (userService.existsByUsername(user.getUsername())) {
                model.addAttribute("error", "Username already exists!");
                return "register";
            }
            
            if (userService.existsByEmail(user.getEmail())) {
                model.addAttribute("error", "Email already exists!");
                return "register";
            }
            
            userService.registerCustomer(user.getUsername(), user.getPassword(), 
                                       user.getEmail(), user.getFirstName(), user.getLastName());
            
            model.addAttribute("message", "Registration successful! Please login.");
            return "login";
            
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
    	try {
	        for (GrantedAuthority authority : authentication.getAuthorities()) {
	            if (authority.getAuthority().equals("ROLE_ADMIN")) {
	                return "redirect:/admin/dashboard";
	            } else if (authority.getAuthority().equals("ROLE_CUSTOMER")) {
	            	System.out.println(authority.getAuthority());
	                return "redirect:/customer/dashboard";
	            }
	        }
	        return "redirect:/login";
    	} catch (Exception e) {
    		System.out.println("Error in dashboard redirection: " + e.getMessage());
    		return "redirect:/login"; 
    	}
    }
    
    @GetMapping("/access-denied")
    public String accessDenied() {
    	System.out.println("Access denied page reached");
        return "access-denied";
    }
}
