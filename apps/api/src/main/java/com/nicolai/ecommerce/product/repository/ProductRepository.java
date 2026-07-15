package com.nicolai.ecommerce.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nicolai.ecommerce.product.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
