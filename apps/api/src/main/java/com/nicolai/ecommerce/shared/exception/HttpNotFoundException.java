package com.nicolai.ecommerce.shared.exception;

public class HttpNotFoundException extends RuntimeException {

  public HttpNotFoundException(String message) {
    super(message);
  }

}
