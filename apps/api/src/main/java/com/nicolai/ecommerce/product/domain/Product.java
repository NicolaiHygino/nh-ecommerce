package com.nicolai.ecommerce.product.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Product
 */
@Entity
@Table(name = "products")
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "cents_price", nullable = false)
  private Long cents;

  @Column(nullable = false)
  private Integer stock;

  @Column(name = "image_url", length = 512)
  private String imageUrl;

  @Column(nullable = false)
  private Boolean ativo;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  protected Product() {
  }

  public Product(String name, String description, Long cents, Integer stock, String imageUrl, Boolean ativo) {
    this.name = name;
    this.description = description;
    this.cents = cents;
    this.stock = stock;
    this.imageUrl = imageUrl;
    this.ativo = ativo;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Long getCents() {
    return cents;
  }

  public Integer getStock() {
    return stock;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public Boolean getAtivo() {
    return ativo;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

}
