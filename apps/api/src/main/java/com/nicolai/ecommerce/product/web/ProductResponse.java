package com.nicolai.ecommerce.product.web;

import java.time.LocalDateTime;

import com.nicolai.ecommerce.product.domain.Product;

public record ProductResponse(
    Long id,
    String name,
    String description,
    Long cents,
    Integer stock,
    String imageUrl,
    Boolean ativo,
    LocalDateTime createdAt) {

  public static ProductResponse from(Product product) {
    return new ProductResponse(
        product.getId(),
        product.getName(),
        product.getDescription(),
        product.getCents(),
        product.getStock(),
        product.getImageUrl(),
        product.getAtivo(),
        product.getCreatedAt());
  }

}
