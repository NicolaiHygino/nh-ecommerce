package com.nicolai.ecommerce.product.domain.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nicolai.ecommerce.product.domain.CreateProductInput;
import com.nicolai.ecommerce.product.domain.InvalidProductException;
import com.nicolai.ecommerce.product.domain.Product;
import com.nicolai.ecommerce.product.repository.ProductRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class CreateProductUseCaseTest {

  @Mock
  private ProductRepository productRepository;

  private CreateProductUseCase useCase;

  @BeforeEach
  void setUp() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    useCase = new CreateProductUseCase(productRepository, validator);
  }

  @Test
  void createsProductWhenInputIsValid() {
    CreateProductInput input = new CreateProductInput("Mouse", "Wireless mouse", 5000L, 10, "http://img", true);
    Product saved = new Product("Mouse", "Wireless mouse", 5000L, 10, "http://img", true);
    when(productRepository.save(any(Product.class))).thenReturn(saved);

    Product result = useCase.execute(input);

    ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
    verify(productRepository).save(captor.capture());
    Product captured = captor.getValue();
    assertEquals("Mouse", captured.getName());
    assertEquals(5000L, captured.getCents());
    assertEquals(10, captured.getStock());
    assertEquals(saved, result);
  }

  @Test
  void throwsConstraintViolationExceptionWhenNameIsBlank() {
    CreateProductInput input = new CreateProductInput("", "desc", 5000L, 10, null, true);

    assertThrows(ConstraintViolationException.class, () -> useCase.execute(input));
    verify(productRepository, never()).save(any());
  }

  @Test
  void throwsInvalidProductExceptionWhenStockIsNegative() {
    CreateProductInput input = new CreateProductInput("Mouse", "desc", 5000L, -1, null, true);

    assertThrows(InvalidProductException.class, () -> useCase.execute(input));
    verify(productRepository, never()).save(any());
  }

}
