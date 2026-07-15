package com.nicolai.ecommerce.product.domain.usecase;

import org.springframework.stereotype.Component;

import com.nicolai.ecommerce.product.domain.Product;
import com.nicolai.ecommerce.product.domain.ProductNotFoundException;
import com.nicolai.ecommerce.product.repository.ProductRepository;

@Component
public class FindProductByIdUseCase {

  private final ProductRepository productRepository;

  public FindProductByIdUseCase(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  public Product execute(Long id) {
    return productRepository.findById(id)
        .orElseThrow(() -> new ProductNotFoundException(id));
  }

}
