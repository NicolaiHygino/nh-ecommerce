package com.nicolai.ecommerce.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * OrderItem
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @NotNull
  @Column(name = "product_id", nullable = false)
  private Long productId;

  @NotNull
  @Positive
  @Column(name = "quantidade", nullable = false)
  private Integer quantity;

  @NotNull
  @Positive
  @Column(name = "preco_unitario_centavos", nullable = false)
  private Long unitPriceCents;

  public OrderItem() {
  }

}
