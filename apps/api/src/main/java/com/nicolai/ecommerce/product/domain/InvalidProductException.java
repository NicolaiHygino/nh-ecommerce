package com.nicolai.ecommerce.product.domain;

import com.nicolai.ecommerce.shared.exception.HttpBadRequestException;

public class InvalidProductException extends HttpBadRequestException {

  public InvalidProductException(String message) {
    super(message);
  }

}
