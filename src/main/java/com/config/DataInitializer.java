package com.config;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bean.*;
import com.repository.CategoryRepository;
import com.repository.ProductRepository;
import com.service.UserService;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Override
    public void run(String... args) throws Exception {
        // to create a default admin user if it does not exist
        if (!userService.existsByUsername("admin")) {
            userService.createAdmin("admin", "admin@123", "admin@gmail.com");
            System.out.println("Default admin user created: username=admin, password=admin123");
        }
        
        createDefaultCategories();
        createDefaultProducts();
    }
    
    private void createDefaultCategories() {
        String[] categories = {"Running Shoes", "Basketball Shoes", "Casual Shoes", "Formal Shoes", "Boots", "Flip Flops",};
        
        for (String categoryName : categories) {
            if (categoryRepository.findByName(categoryName).isEmpty()) {
                Category category = new Category();
                category.setName(categoryName);
                category.setDescription("Life Wear "+categoryName); 
                categoryRepository.save(category);
            }
        }
        
        System.out.println("Default categories created if they did not exist.");
    }
    private void createDefaultProducts() {
		String[] productNames = {
			"Nike Air Zoom Pegasus 37", 
			"Adidas Ultraboost 21", 
		};
		String[] productSizes = {
			"7", "8"
		};
		String[] productColors = {
			"Black", "White"
		};
		String[] productBrands = {
			"Nike", "Adidas"
		};
		String[] productImages = {
			"https://images.unsplash.com/photo-1542291026-7eec264c27ff?q=80&w=1170&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D", 
			"https://images.unsplash.com/photo-1529810313688-44ea1c2d81d3?q=80&w=641&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
		};
		
		for (int i=0; i < productNames.length; i++) {
			if (productRepository.findByName(productNames[i]).isEmpty()) {
				Product product = new Product();
				product.setName(productNames[i]);
				product.setDescription("High performance " + productNames[i]);
				product.setPrice(new BigDecimal("5499"));
				product.setBrand(productBrands[i % productBrands.length]);
				product.setSize(productSizes[i % productSizes.length]);
				product.setColor(productColors[i % productColors.length]);
				product.setStockQuantity(100);
				product.setImageUrl(productImages[i % productImages.length]);
				
				Category category = categoryRepository.findByName("Running Shoes").orElse(null);
				if (category != null) {
					product.setCategory(category);
					productRepository.save(product);
				}
			}
		}
		
		System.out.println("Default products created if they did not exist.");
	}
    
}
