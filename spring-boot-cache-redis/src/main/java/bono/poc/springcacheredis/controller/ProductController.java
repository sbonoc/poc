package bono.poc.springcacheredis.controller;

import bono.poc.springcacheredis.dto.ProductDto;
import bono.poc.springcacheredis.mapper.ProductMapper;
import bono.poc.springcacheredis.service.ProductService;
import io.micrometer.core.annotation.Timed;
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

    @Timed
    @GetMapping("/product-api-without-cache/{id}")
    public ResponseEntity<ProductDto> getProductByIdWithoutCache(@PathVariable String id) {
        return ResponseEntity.of(productService.getProductFromRedisWithoutCache(id).map(productMapper::modelToDto));
    }

    @Timed
    @GetMapping("/product-api-with-cache/{id}")
    public ResponseEntity<ProductDto> getProductByIdWithCache(@PathVariable String id) {
        return ResponseEntity.of(productService.getProductFromRedisWithCache(id).map(productMapper::modelToDto));
    }

}
