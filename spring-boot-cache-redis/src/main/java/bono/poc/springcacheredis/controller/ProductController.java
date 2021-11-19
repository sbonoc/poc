package bono.poc.springcacheredis.controller;

import bono.poc.springcacheredis.dto.ProductDto;
import bono.poc.springcacheredis.mapper.ProductMapper;
import bono.poc.springcacheredis.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    @GetMapping("/products")
    public ResponseEntity<List<ProductDto>> getProducts() {
        return ResponseEntity.ok(
                productService.getProductsWithoutCache().stream()
                        .map(productMapper::modelToDto)
                        .collect(Collectors.toList())
        );
    }

    @GetMapping("/product-api-without-cache/{id}")
    public ResponseEntity<ProductDto> getProductByIdWithoutCache(@PathVariable String id) {
        return ResponseEntity.of(productService.getProductWithoutCache(id).map(productMapper::modelToDto));
    }

    @GetMapping("/product-api-using-caffeine-cache/{id}")
    public ResponseEntity<ProductDto> getProductByIdUsingCaffeineCache(@PathVariable String id) {
        return ResponseEntity.of(productService.getProductWithCache(id).map(productMapper::modelToDto));
    }

}
