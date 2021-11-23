package bono.poc.springcacheredis.controller;

import bono.poc.springcacheredis.dto.ProductDto;
import bono.poc.springcacheredis.mapper.ProductMapper;
import bono.poc.springcacheredis.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    @GetMapping("/product-api-without-cache/{id}")
    public ResponseEntity<ProductDto> getProductByIdWithoutCache(@PathVariable String id) {
        return ResponseEntity.of(productService.getProductWithoutCache(id).map(productMapper::modelToDto));
    }

    @GetMapping("/product-api-with-cache/{id}")
    public ResponseEntity<ProductDto> getProductByIdUsingCaffeineCache(@PathVariable String id) {
        return ResponseEntity.of(productService.getProductWithCache(id).map(productMapper::modelToDto));
    }

}
