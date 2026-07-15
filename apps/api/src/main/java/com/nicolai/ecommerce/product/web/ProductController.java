package com.nicolai.ecommerce.product.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nicolai.ecommerce.product.domain.CreateProductInput;
import com.nicolai.ecommerce.product.domain.Product;
import com.nicolai.ecommerce.product.domain.usecase.CreateProductUseCase;
import com.nicolai.ecommerce.product.domain.usecase.FindProductByIdUseCase;

@RestController
@RequestMapping("/products")
public class ProductController {

  private final CreateProductUseCase createProductUseCase;
  private final FindProductByIdUseCase findProductByIdUseCase;

  public ProductController(CreateProductUseCase createProductUseCase, FindProductByIdUseCase findProductByIdUseCase) {
    this.createProductUseCase = createProductUseCase;
    this.findProductByIdUseCase = findProductByIdUseCase;
  }

  @PostMapping
  public ResponseEntity<ProductResponse> create(@RequestBody CreateProductInput input) {
    Product product = createProductUseCase.execute(input);
    return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ProductResponse> findById(@PathVariable Long id) {
    Product product = findProductByIdUseCase.execute(id);
    return ResponseEntity.ok(ProductResponse.from(product));
  }

}
