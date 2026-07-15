package com.nicolai.ecommerce.product.domain.usecase;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.nicolai.ecommerce.product.domain.CreateProductInput;
import com.nicolai.ecommerce.product.domain.InvalidProductException;
import com.nicolai.ecommerce.product.domain.Product;
import com.nicolai.ecommerce.product.repository.ProductRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Component
public class CreateProductUseCase {

  private final ProductRepository productRepository;
  private final Validator validator;

  public CreateProductUseCase(ProductRepository productRepository, Validator validator) {
    this.productRepository = productRepository;
    this.validator = validator;
  }

  public Product execute(CreateProductInput input) {
    Set<ConstraintViolation<CreateProductInput>> violations = validator.validate(input);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }

    if (input.stock() < 0) {
      throw new InvalidProductException("Stock cannot be negative");
    }

    Product product = new Product(
        input.name(),
        input.description(),
        input.cents(),
        input.stock(),
        input.imageUrl(),
        input.ativo());

    return productRepository.save(product);
  }

}
