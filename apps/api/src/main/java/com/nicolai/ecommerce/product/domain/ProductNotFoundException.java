package com.nicolai.ecommerce.product.domain;

import com.nicolai.ecommerce.shared.exception.HttpNotFoundException;

public class ProductNotFoundException extends HttpNotFoundException {

  public ProductNotFoundException(Long id) {
    super("Product not found: " + id);
  }

}
