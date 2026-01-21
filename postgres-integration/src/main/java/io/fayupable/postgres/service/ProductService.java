package io.fayupable.postgres.service;

import io.fayupable.postgres.dto.request.CreateProductRequest;
import io.fayupable.postgres.dto.request.UpdateProductRequest;
import io.fayupable.postgres.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse getProductById(Long id);

    Page<ProductResponse> getAllProducts(Pageable pageable);

    ProductResponse updateProduct(Long id, UpdateProductRequest request);

    void deleteProduct(Long id);
}