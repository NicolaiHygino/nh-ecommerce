package com.nicolai.ecommerce.shared.exception;

public class HttpBadRequestException extends RuntimeException {

  public HttpBadRequestException(String message) {
    super(message);
  }

}
