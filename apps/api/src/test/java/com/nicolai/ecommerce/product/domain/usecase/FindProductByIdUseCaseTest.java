package com.nicolai.ecommerce.product.domain.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nicolai.ecommerce.product.domain.Product;
import com.nicolai.ecommerce.product.domain.ProductNotFoundException;
import com.nicolai.ecommerce.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class FindProductByIdUseCaseTest {

  @Mock
  private ProductRepository productRepository;

  @InjectMocks
  private FindProductByIdUseCase useCase;

  @Test
  void returnsProductWhenIdExists() {
    Product product = new Product("Mouse", "desc", 5000L, 10, null, true);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    Product result = useCase.execute(1L);

    assertEquals(product, result);
    verify(productRepository).findById(1L);
  }

  @Test
  void throwsProductNotFoundExceptionWhenIdDoesNotExist() {
    when(productRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(ProductNotFoundException.class, () -> useCase.execute(99L));
    verify(productRepository).findById(99L);
  }

}
