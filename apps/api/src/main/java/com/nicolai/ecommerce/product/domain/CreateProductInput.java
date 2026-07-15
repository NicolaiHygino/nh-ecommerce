package com.nicolai.ecommerce.product.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProductInput(
    @NotBlank String name,
    String description,
    @NotNull Long cents,
    @NotNull Integer stock,
    String imageUrl,
    Boolean ativo) {
}
