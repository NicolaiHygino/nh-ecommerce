package com.nicolai.ecommerce.product.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nicolai.ecommerce.product.domain.CreateProductInput;
import com.nicolai.ecommerce.product.domain.InvalidProductException;
import com.nicolai.ecommerce.product.domain.Product;
import com.nicolai.ecommerce.product.domain.ProductNotFoundException;
import com.nicolai.ecommerce.product.domain.usecase.CreateProductUseCase;
import com.nicolai.ecommerce.product.domain.usecase.FindProductByIdUseCase;
import com.nicolai.ecommerce.shared.config.SecurityConfig;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CreateProductUseCase createProductUseCase;

  @MockitoBean
  private FindProductByIdUseCase findProductByIdUseCase;

  @Test
  void createsProductAndReturns201() throws Exception {
    Product product = new Product("Mouse", "Wireless mouse", 5000L, 10, null, true);
    when(createProductUseCase.execute(any(CreateProductInput.class))).thenReturn(product);

    mockMvc.perform(post("/products")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"name":"Mouse","description":"Wireless mouse","cents":5000,"stock":10,"ativo":true}
            """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Mouse"))
        .andExpect(jsonPath("$.cents").value(5000));
  }

  @Test
  void findsProductByIdAndReturns200() throws Exception {
    Product product = new Product("Mouse", "Wireless mouse", 5000L, 10, null, true);
    when(findProductByIdUseCase.execute(eq(1L))).thenReturn(product);

    mockMvc.perform(get("/products/{id}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Mouse"))
        .andExpect(jsonPath("$.cents").value(5000));
  }

  @Test
  void returns404WhenProductDoesNotExist() throws Exception {
    when(findProductByIdUseCase.execute(eq(99L))).thenThrow(new ProductNotFoundException(99L));

    mockMvc.perform(get("/products/{id}", 99L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Product not found: 99"));
  }

  @Test
  void returns400WhenValidationFails() throws Exception {
    CreateProductInput invalidInput = new CreateProductInput("", "desc", 5000L, 10, null, true);
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<CreateProductInput>> violations = validator.validate(invalidInput);
    when(createProductUseCase.execute(any(CreateProductInput.class)))
        .thenThrow(new ConstraintViolationException(violations));

    mockMvc.perform(post("/products")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"name":"","description":"desc","cents":5000,"stock":10,"ativo":true}
            """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void returns400WhenStockIsNegative() throws Exception {
    when(createProductUseCase.execute(any(CreateProductInput.class)))
        .thenThrow(new InvalidProductException("Stock cannot be negative"));

    mockMvc.perform(post("/products")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"name":"Mouse","description":"desc","cents":5000,"stock":-1,"ativo":true}
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Stock cannot be negative"));
  }

}
